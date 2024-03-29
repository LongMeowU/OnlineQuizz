package controller.user.profile;

import dao.RoomDBContext;
import dao.SetDBContext;
import dao.UserDBContext;
import entity.Room;
import entity.Set;
import entity.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

public class UserProfile extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        UserDBContext udb = new UserDBContext();
        User user = null;
        if (request.getParameter("uId") != null) {
            int uId = Integer.parseInt(request.getParameter("uId"));
            user = udb.get(uId);
        } else {
            HttpSession session = request.getSession();
            user = (User) session.getAttribute("user");
        }
        //User
        int countSet = udb.CountSet(user.getId());
        int countRoom = udb.CountRoom(user.getId());
        // Set
        SetDBContext sdb = new SetDBContext();
        List<Set> listS = sdb.getOwnedSet(user);
        // Room
        RoomDBContext rdb = new RoomDBContext();
        List<Room> listR = rdb.list(user);

        // close connection
        try {
            udb.closeConnection();
            sdb.closeConnection();
            rdb.closeConnection();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        request.setAttribute("user", user);
        request.setAttribute("countSet", countSet);
        request.setAttribute("countRoom", countRoom);
        request.setAttribute("listS", listS);
        request.setAttribute("listR", listR);
        request.getRequestDispatcher("../view/user/profile/UserProfile.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }
}
