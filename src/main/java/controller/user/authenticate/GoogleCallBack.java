package controller.user.authenticate;

import com.google.api.client.auth.oauth2.*;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dao.NotificationDBContext;
import dao.NotificationTypeDBContext;
import dao.RoleDBContext;
import dao.UserDBContext;
import entity.Notification;
import entity.NotificationType;
import entity.Role;
import entity.User;
import io.github.cdimascio.dotenv.Dotenv;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import websocket.endpoints.AdminDashboardWebSocketEndpoint;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GoogleCallBack extends HttpServlet {
    Dotenv dotenv = Dotenv.configure().load();
    private String GOOGLE_CLIENT_ID = dotenv.get("GOOGLE_CLIENT_ID");
    private String GOOGLE_CLIENT_SECRET = dotenv.get("GOOGLE_CLIENT_SECRET");
    private String GOOGLE_LINK_GET_TOKEN = dotenv.get("GOOGLE_LINK_GET_TOKEN");
    private String GOOGLE_LINK_AUTH = dotenv.get("GOOGLE_LINK_AUTH");
    private String GOOGLE_REDIRECT_URI = dotenv.get("GOOGLE_REDIRECT_URI");
    private String[] GOOGLE_SCOPES = dotenv.get("GOOGLE_SCOPES").split(", ");
    private AuthorizationCodeFlow flow;
    private final Lock lock = new ReentrantLock();


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        StringBuffer buf = req.getRequestURL();
        if (req.getQueryString() != null) {
            buf.append('?').append(req.getQueryString());
        }
        AuthorizationCodeResponseUrl responseUrl = new AuthorizationCodeResponseUrl(buf.toString());
        String code = responseUrl.getCode();
        if (responseUrl.getError() != null) {
            onError(req, resp, responseUrl);
        } else if (code == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().print("Missing authorization code");
        } else {
            lock.lock();
            try {
                if (flow == null) {
                    flow = initializeFlow();
                }
                String redirectUri = getRedirectUri(req);
                TokenResponse response = flow.newTokenRequest(code).setRedirectUri(redirectUri).execute();
                Credential credential = flow.createAndStoreCredential(response, null);
                onSuccess(req, resp, credential);
            } finally {
                lock.unlock();
            }
        }

    }

    private String getRedirectUri(HttpServletRequest request) {
        return GOOGLE_REDIRECT_URI;
    }

    private void onError(HttpServletRequest request, HttpServletResponse response, AuthorizationCodeResponseUrl errorResponse) throws IOException {
        response.getWriter().println("login error!");
    }

    private void onSuccess(HttpServletRequest request, HttpServletResponse response, Credential credential) throws IOException {
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet httpGet = new HttpGet("https://www.googleapis.com/userinfo/v2/me");
        httpGet.setHeader("Authorization", "Bearer " + credential.getAccessToken());
        HttpResponse httpResponse = client.execute(httpGet);
        HttpEntity entity = httpResponse.getEntity();
        if (entity != null) {
            UserDBContext db = new UserDBContext();
            // Convert the response content to a string
            String responseBody = EntityUtils.toString(entity);
            JsonObject jsonObject = new Gson().fromJson(responseBody, JsonObject.class);
            String email = jsonObject.get("email").getAsString();
            String givenName = jsonObject.get("given_name").getAsString();
            String familyName = jsonObject.get("family_name").getAsString();
            String picture = jsonObject.get("picture").getAsString();
            boolean verified = jsonObject.get("verified_email").getAsBoolean();
            User user = new User();
            user.setEmail(email);
            user.setGivenName(givenName);
            user.setFamilyName(familyName);
            user.setAvatar(picture);
            user.setVerified(verified);
//            try {
//                RoleDBContext roleDBContext = new RoleDBContext();
//                user.setRoles(roleDBContext.list(user.getEmail()));
//                roleDBContext.closeConnection();
//            } catch (ClassNotFoundException ex) {
//                Logger.getLogger(LoginController.class.getName()).log(Level.SEVERE, null, ex);
//            } catch (SQLException e) {
//                throw new RuntimeException(e);
//            }
            // Check if user is already in database, if not, insert
            if (db.checkEmail(user.getEmail())) {
                try {
                    db.insert(user);
                    User userAfterInsert = db.get(email);
                    ArrayList<User> tos = db.getAdmin("Administrator");
                    Notification notification = createNotification(tos, userAfterInsert);
                    NotificationDBContext notificationDBContext = new NotificationDBContext();
                    notificationDBContext.insert(notification);
                    AdminDashboardWebSocketEndpoint.notifyAdminsNewUserRegistered(notification);
                    // close connection
                    notificationDBContext.closeConnection();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            }
            User registeredUser = db.get(user.getEmail());
            HttpSession session = request.getSession();
            session.setAttribute("user", registeredUser);
            Boolean isAdmin = false;
            if (registeredUser != null) {
                for (Role role : registeredUser.getRoles()) {
                    if (role.getName().equals("Administrator")) {
                        isAdmin = true;
                        break;
                    }
                }
                session.setAttribute("isAdmin", isAdmin);
            }
            // close connection
            try {
                db.closeConnection();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            if (isAdmin) {
                response.sendRedirect("./admin/dashboard");
            } else {
                response.sendRedirect("./");
            }
        } else {
            // Handle the case where the response entity is null
            response.getWriter().println("Error: No response entity");
        }

    }

    private AuthorizationCodeFlow initializeFlow() throws IOException {
        return new AuthorizationCodeFlow.Builder(
                // Provide the necessary parameters based on your configuration
                BearerToken.authorizationHeaderAccessMethod(),  // Replace with appropriate access method if needed
                new NetHttpTransport(),  // Replace with your preferred HttpTransport instance
                new GsonFactory(),  // Assuming you have a JSON_FACTORY object
                new GenericUrl(GOOGLE_LINK_GET_TOKEN),
                new ClientParametersAuthentication(GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET),
                GOOGLE_CLIENT_ID,
                GOOGLE_LINK_AUTH
        ).setScopes(Arrays.asList(GOOGLE_SCOPES)).build();
    }

    private Notification createNotification(ArrayList<User> tos, User from) {
        Notification notification = new Notification();
        NotificationTypeDBContext notificationTypeDBContext = new NotificationTypeDBContext();
        notification.setRead(false);
        NotificationType notificationType = notificationTypeDBContext.get(1);
        notification.setType(notificationType);
        notification.setTos(tos);
        notification.setFrom(from);
        notification.setContent(notificationType.getAction() + from.getEmail());
        notification.setUrl("/Quizzicle/admin/user/profile?uid=" + from.getId());
        return notification;
    }
}