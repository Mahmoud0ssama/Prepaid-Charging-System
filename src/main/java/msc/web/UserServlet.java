package msc.web;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import msc.DatabaseManager;
import msc.DatabaseManager.User;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

public class UserServlet extends HttpServlet {
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("[UserServlet] doGet called with query=" + req.getQueryString());
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String msisdn = req.getParameter("msisdn");
        if (msisdn != null && !msisdn.trim().isEmpty()) {
            BigDecimal balance = DatabaseManager.getBalance(msisdn);
            if (DatabaseManager.userExists(msisdn)) {
                // Find and return detailed user information
                List<User> users = DatabaseManager.getAllUsers();
                for (User u : users) {
                    if (u.msisdn.equals(msisdn)) {
                        resp.getWriter().write(gson.toJson(u));
                        return;
                    }
                }
                // Fallback user details
                resp.getWriter().write(gson.toJson(new User(0, msisdn, balance)));
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("{\"error\": \"User not found\"}");
            }
        } else {
            List<User> users = DatabaseManager.getAllUsers();
            resp.getWriter().write(gson.toJson(users));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        try {
            BufferedReader reader = req.getReader();
            JsonObject data = gson.fromJson(reader, JsonObject.class);

            if (data == null || !data.has("msisdn") || !data.has("balance")) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"error\": \"Missing msisdn or balance\"}");
                return;
            }

            String msisdn = data.get("msisdn").getAsString().trim();
            BigDecimal balance = data.get("balance").getAsBigDecimal();

            if (msisdn.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"error\": \"MSISDN cannot be empty\"}");
                return;
            }

            if (DatabaseManager.userExists(msisdn)) {
                resp.setStatus(HttpServletResponse.SC_CONFLICT);
                resp.getWriter().write("{\"error\": \"MSISDN already exists\"}");
                return;
            }

            boolean success = DatabaseManager.createUser(msisdn, balance);
            if (success) {
                resp.setStatus(HttpServletResponse.SC_CREATED);
                resp.getWriter().write("{\"message\": \"User created successfully\"}");
            } else {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("{\"error\": \"Failed to create user\"}");
            }
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        try {
            BufferedReader reader = req.getReader();
            JsonObject data = gson.fromJson(reader, JsonObject.class);

            if (data == null || !data.has("id") || !data.has("msisdn") || !data.has("balance")) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"error\": \"Missing id, msisdn, or balance\"}");
                return;
            }

            int id = data.get("id").getAsInt();
            String msisdn = data.get("msisdn").getAsString().trim();
            BigDecimal balance = data.get("balance").getAsBigDecimal();

            if (msisdn.isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"error\": \"MSISDN cannot be empty\"}");
                return;
            }

            boolean success = DatabaseManager.updateUser(id, msisdn, balance);
            if (success) {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("{\"message\": \"User updated successfully\"}");
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("{\"error\": \"User not found or no changes made\"}");
            }
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String idStr = req.getParameter("id");
        if (idStr == null || idStr.trim().isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\": \"Missing user id parameter\"}");
            return;
        }

        try {
            int id = Integer.parseInt(idStr);
            boolean success = DatabaseManager.deleteUser(id);
            if (success) {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.getWriter().write("{\"message\": \"User deleted successfully\"}");
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("{\"error\": \"User not found\"}");
            }
        } catch (NumberFormatException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\": \"Invalid user id format\"}");
        }
    }
}
