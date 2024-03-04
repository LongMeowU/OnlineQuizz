package controller.user.set;


import dao.SetDBContext;
import entity.Set;
import entity.User;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.ArrayList;


public class DeleteSet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");
        int setId = Integer.parseInt(request.getParameter("set-id"));
        SetDBContext setDB = new SetDBContext();
        //check user is owner of set
        if (setDB.isOwner(user.getId(), setId)) {
            setDB.deleteBySetID(setId);
            response.sendRedirect("viewAll");
        } else {
            response.getWriter().println("You are not owner of this set");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }
}