package controller.user.room.test;

import dao.QuestionDBContext;
import dao.TestDBContext;
import dao.TestQuestionDBContext;
import entity.Question;
import entity.Room;
import entity.Test;
import entity.TestQuestion;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import util.DateTimeLocalConverter;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;

public class UpdateTest extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        int testId = Integer.parseInt(request.getParameter("testId"));
        TestDBContext testDB = new TestDBContext();
        Test test = new Test();
        test.setTestId(testId);
        test = testDB.getTestById(test);
        TestQuestionDBContext testQuestionDB = new TestQuestionDBContext();
        ArrayList<TestQuestion> testQuestions = testQuestionDB.list(testId);
        QuestionDBContext questionDB = new QuestionDBContext();
        ArrayList<Question> questions = questionDB.list(testQuestions);
        float totalScore = 0;
        for (TestQuestion testQuestion : testQuestions) {
            totalScore += testQuestion.getScore();
        }
        request.setAttribute("testQuestions", testQuestions);
        request.setAttribute("totalScore", totalScore);
        request.setAttribute("test", test);
        request.setAttribute("questions", questions);
        //close connection

        request.getRequestDispatcher("../../.././view/user/room/test/UpdateTest.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        int testId = Integer.parseInt(request.getParameter("testId"));
        int roomId = Integer.parseInt(request.getParameter("roomId"));
        String Name = request.getParameter("name");
        String Description = request.getParameter("description");
        Timestamp startTime = DateTimeLocalConverter.DateTimeLocalToTimestamp(request.getParameter("start"));
        int duration = Integer.parseInt(request.getParameter("duration"));
        int attempt = Integer.parseInt(request.getParameter("attempt"));
        Timestamp endTime = DateTimeLocalConverter.DateTimeLocalToTimestamp(request.getParameter("end"));
        Test test = new Test();
        test.setTestName(Name);
        test.setTestDescription(Description);
        test.setDuration(duration);
        test.setAttempt(attempt);
        test.setStartTime(startTime);
        test.setEndTime(endTime);
        test.setTestId(testId);
        Room room = new Room();
        room.setRoomId(roomId);
        test.setRoom(room);


        String[] questions = request.getParameterValues("question-ids");
        ArrayList<TestQuestion> testQuestions = new ArrayList<>();
        for (int i = 0; i < questions.length; i++) {
            float score = Float.parseFloat(request.getParameter("score-question-" + questions[i]));
            int questionId = Integer.parseInt(questions[i]);
            TestQuestion testQuestion = new TestQuestion();
            testQuestion.setQId(questionId);
            testQuestion.setScore(score);
            testQuestion.setTestId(testId);
            testQuestions.add(testQuestion);
        }
        // update test to database
        TestDBContext testDBContext = new TestDBContext();
        testDBContext.update(test, testQuestions);
        response.sendRedirect("../../room/get?roomId=" + room.getRoomId());
    }


}
