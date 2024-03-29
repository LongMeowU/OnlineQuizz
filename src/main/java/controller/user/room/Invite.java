package controller.user.room;

import dao.NotificationDBContext;
import dao.NotificationTypeDBContext;
import entity.Notification;
import entity.NotificationType;
import util.GenerateCodeToJoin;
import dao.RoomDBContext;
import dao.UserDBContext;
import entity.Room;
import entity.User;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Invite extends HttpServlet {
    private static Map<Room, String> roomCodes = new HashMap<>();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        UserDBContext uDB = new UserDBContext();
        User u;
        User userLogged = (User) request.getSession().getAttribute("user");

        u = uDB.get(userLogged.getEmail());
        String fakeCode = request.getParameter("codeToJoin");
        // invite?codeToJoin=1231231231231123
        RoomDBContext rDB = new RoomDBContext();
        ArrayList<Room> listRoom = rDB.listAllRoomExceptOwner(u);
        for (Room room : listRoom) {
            String hashCode = GenerateCodeToJoin.generateCode(room.getCode() + room.getPassword());
            roomCodes.put(room, hashCode);
        }
        Room r = findRoomByFakeCode(fakeCode, roomCodes);
        if (r != null) {
            // insert to Many-Many table (uid, roomid) = (ownerUserId, r.room_id)
            rDB.insertIntoUser_Join_Room(u.getId(), r.getRoomId());
            Notification notification = createNotification(request, r);
            NotificationDBContext notificationDBContext = new NotificationDBContext();
            notificationDBContext.insert(notification);
            // close connection
            try {
                rDB.closeConnection();
                uDB.closeConnection();
                notificationDBContext.closeConnection();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            response.sendRedirect("../../../Quizzicle/user/room");
        } else {
            request.getRequestDispatcher("/view/user/RoomScreen/NotFound.jsp").forward(request, response);
        }

        // close connection
        try {
            rDB.closeConnection();
            uDB.closeConnection();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        UserDBContext uDB = new UserDBContext();
        User u;
        User userLogged = (User) request.getSession().getAttribute("user");
        u = uDB.get(userLogged.getEmail());
        String code = request.getParameter("code");
        String passwordForJoining = request.getParameter("passwordForJoining");
        RoomDBContext rDB = new RoomDBContext();
        // Check exist room
        Room r;
        r = rDB.getRoomToJoin(code, passwordForJoining);
        if (r != null) {
            // insert to Many-Many table (uid, roomid) = (ownerUserId, r.room_id)
            rDB.insertIntoUser_Join_Room(u.getId(), r.getRoomId());
            Notification notification = createNotification(request, r);
            NotificationDBContext notificationDBContext = new NotificationDBContext();
            notificationDBContext.insert(notification);
            response.sendRedirect("../../../Quizzicle/user/room");
        } else {
            request.getRequestDispatcher("/view/user/RoomScreen/NotFound.jsp").forward(request, response);
        }
    }

    private static Room findRoomByFakeCode(String fakeCode, Map<Room, String> roomCodes) {
        for (Map.Entry<Room, String> entry : roomCodes.entrySet()) {
            if (entry.getValue().equals(fakeCode)) {
                return entry.getKey();
            }
        }
        return null; // No room found for the given fakeCode
    }

    private Notification createNotification(HttpServletRequest request, Room room) {
        NotificationTypeDBContext notificationTypeDBContext = new NotificationTypeDBContext();
        RoomDBContext roomDBContext = new RoomDBContext();
        HttpSession session = request.getSession();
        User from = (User) session.getAttribute("user");
        Room fullInfoRoom = roomDBContext.getRoomById(room);
        Notification notification = new Notification();
        NotificationType notificationType = notificationTypeDBContext.get(12);
        notification.setType(notificationType);
        notification.setFrom(from);
        ArrayList<User> tos = new ArrayList<>();
        tos.add(fullInfoRoom.getUser());
        notification.setTos(tos);
        notification.setRead(false);
        notification.setUrl("/Quizzicle/user/room/get?roomId=" + room.getRoomId());
        notification.setContent(from.getFamilyName() + " " +
                from.getGivenName() + " " + notificationType.getAction() + " " + room.getRoomName());

        return notification;
    }
}
