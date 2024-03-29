package controller.admin.room;

import dao.NotificationDBContext;
import dao.NotificationTypeDBContext;
import dao.RoomDBContext;
import dao.UserDBContext;
import entity.Notification;
import entity.Room;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.ArrayList;
import entity.User;

public class DeleteRoom extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        int roomId = Integer.parseInt(request.getParameter("room_id"));
        RoomDBContext roomDBContext = new RoomDBContext();
        NotificationDBContext notificationDBContext = new NotificationDBContext();
//        UserDBContext userDBContext = new UserDBContext();
        NotificationTypeDBContext notificationTypeDBContext = new NotificationTypeDBContext();

        Room room = new Room();
        room.setRoomId(roomId);
        Room roomToDelete = roomDBContext.getRoomById(room);

        Notification notification = new Notification();
        notification.setRead(false);
        User from = (User)request.getSession().getAttribute("user");
        notification.setFrom(from);
        ArrayList<User> tos = new ArrayList<>();
        tos.add(roomToDelete.getUser());
        notification.setTos(tos);
        notification.setType(notificationTypeDBContext.get(7));
        notification.setUrl("/Quizzicle/user/room");
        notification.setContent("Admin delete your room: " + roomToDelete.getRoomName());

        notificationDBContext.insert(notification);
        roomDBContext.deleteRoom(room);
        // Close connection
        try {
            roomDBContext.closeConnection();
            notificationDBContext.closeConnection();
//            userDBContext.closeConnection();
            notificationTypeDBContext.closeConnection();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        response.sendRedirect("../room");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }
}
