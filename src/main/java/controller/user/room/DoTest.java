package controller.user.room;

import dao.TestDBContext;
import dao.UserDBContext;
import entity.Question;
import entity.QuestionOption;
import entity.Test;
import entity.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.Console;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.util.*;

public class DoTest extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        TestDBContext testDBContext = new TestDBContext();
        // Retrieve the testValue value
        int testId = Integer.parseInt(request.getParameter("testId"));
        Test currentTest = new Test();
        currentTest.setTestId(testId);
        currentTest = testDBContext.getTestById(currentTest);

        // Check time. If now < start time or now > end time => Cannot do test
        Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());
        // Perform timestamp comparison
        if (currentTimestamp.before(currentTest.getStartTime()) || currentTimestamp.after(currentTest.getEndTime())) {
            // Cannot do the test
            request.setAttribute("timeErrorMessage", "Cannot do the test at this time.");
            request.getRequestDispatcher("../../../view/user/room/NotFound.jsp").forward(request, response);
            return;
        }
        // Get current user
        User userLogged = (User) request.getSession().getAttribute("user");
        UserDBContext uDB = new UserDBContext();
        User u = uDB.get(userLogged.getEmail());
        // if assume this is owner of this test. We need to find the owner of this room
        // And check if they is one. Restrict them

        User ownerTest = testDBContext.getOwnerTest(currentTest);

        if (u.getId() == ownerTest.getId()) {
            request.setAttribute("NoTestOwnerRights", "You do not have the right to take the test (because you are the owner)");
            request.getRequestDispatcher("../../../view/user/room/NotFound.jsp").forward(request, response);
            return;
        }

        int currentAttempt = testDBContext.getCurrentAttemptOfThisTest(currentTest, userLogged);
        if (currentAttempt >= currentTest.getAttempt()) {
            request.setAttribute("ExceededTimesDoTest", "The number of times you have taken the test has exceeded");
            request.getRequestDispatcher("../../../view/user/room/NotFound.jsp").forward(request, response);
            return;
        }
        currentTest = testDBContext.getTestById(currentTest);

        request.setAttribute("currentTest", currentTest);
        // list question of this test
        ArrayList<Question> listQuestions = testDBContext.getListQuestionsOfTest(currentTest);

        Collections.shuffle(listQuestions);
        // get question by type
        for (Question question : listQuestions) {
            if (question.getType().getTypeName().equals("True/False")) {
                QuestionOption opt1 = new QuestionOption();
                opt1.setOptContent("True");
                QuestionOption opt2 = new QuestionOption();
                opt2.setOptContent("False");
                ArrayList<QuestionOption> opts = new ArrayList<>();
                opts.add(opt1);
                opts.add(opt2);
                question.setQuestionOptions(opts);
            } else if (question.getType().getTypeName().equals("Multiple choice")) {
                ArrayList<QuestionOption> opts = question.getQuestionOptions();
                // add answer to option
                QuestionOption answer = new QuestionOption();
                answer.setOptContent(question.getAnswer());
                opts.add(answer);
                //remove answer from question
                question.setAnswer(null);
                Collections.shuffle(opts);
                question.setQuestionOptions(opts);
            }
        }
        // close connection
        try {
            testDBContext.closeConnection();
            uDB.closeConnection();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        request.setAttribute("listQuestions", listQuestions);
        request.getRequestDispatcher("../../../view/user/room/DoTest.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Get current user
        User userLogged = (User) request.getSession().getAttribute("user");
        UserDBContext uDB = new UserDBContext();
        User u = uDB.get(userLogged.getEmail());

        // Retrieve the testValue value
        int testId = Integer.parseInt(request.getParameter("testId"));
        Test currentTest = new Test();
        currentTest.setTestId(testId);

        // get current attempt
        TestDBContext testDBContext = new TestDBContext();
        int currentAttempt = testDBContext.getCurrentAttemptOfThisTest(currentTest, userLogged);
        currentAttempt++;

        // Get time create at
        Date createdAtDate = new Date();

        Timestamp createdAt = new Timestamp(createdAtDate.getTime());
        testDBContext.insertAttemptDetails(currentTest, userLogged, currentAttempt, createdAt);

        // Create hashmap contain key is question in this test and value is this score
        HashMap<Question, Float> ExactlyAnswerQuestions = new HashMap<Question, Float>();
        ExactlyAnswerQuestions = testDBContext.getExactlyAnswerQuestionsInCertainTest(userLogged, currentTest);

        // Create hashmap contain key is question submitted and it's score after comparing with exactly answer
        HashMap<Question, Float> QuestionSubmittedAndItsScoreAfterComparing = new HashMap<>();


        // Retrieve question IDs and their corresponding answers
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String paramName = parameterNames.nextElement();
            if (paramName.startsWith("answer_")) {
                String questionId = paramName.substring(paramName.indexOf("_") + 1);
                Question q = new Question();
                q.setQId(Integer.parseInt(questionId));
                String answer = request.getParameter(paramName);
                q.setAnswer(answer);
                QuestionSubmittedAndItsScoreAfterComparing.put(q, null);
            }
        }
        // Iterate through ExactlyAnswerQuestions
        for (Map.Entry<Question, Float> entryExactlyAnswer : ExactlyAnswerQuestions.entrySet()) {
            Question exactlyAnswerQuestion = entryExactlyAnswer.getKey();
            Float score = entryExactlyAnswer.getValue();

            // Iterate through QuestionSubmittedAndItsScoreAfterComparing
            for (Map.Entry<Question, Float> entrySubmitted : QuestionSubmittedAndItsScoreAfterComparing.entrySet()) {
                Question submittedQuestion = entrySubmitted.getKey();
                Float submittedScore = entrySubmitted.getValue();

                // Compare if the questions match
                if (exactlyAnswerQuestion.getQId() == submittedQuestion.getQId()) {
                    // Compare if the answers match
                    if (exactlyAnswerQuestion.getAnswer().equals(submittedQuestion.getAnswer())) {
                        // Set the score to the matched question
                        QuestionSubmittedAndItsScoreAfterComparing.put(submittedQuestion, score);
                    } else {
                        // Set the score to 0 for questions with non-matching answers
                        QuestionSubmittedAndItsScoreAfterComparing.put(submittedQuestion, 0f);
                    }
                }
            }
        }
        // Get Id user_does_test corresponding it's attempt
        // Đã có uid, test_id, attempt
        int udt_id = testDBContext.getUserDoesTestId(userLogged, currentTest, currentAttempt);

        // Insert answer
        testDBContext.insertAnswerOfTestWithCorrespondingScore(QuestionSubmittedAndItsScoreAfterComparing, udt_id);

        response.sendRedirect("./leaderboard?testId=" + currentTest.getTestId());
    }
}
