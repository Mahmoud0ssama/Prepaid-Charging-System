package msc.web;
import msc.DatabaseManager;


import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;

public class UserBalanceQuery extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");
        resp.setCharacterEncoding("UTF-8");
        String msisdn = req.getParameter("msisdn");
        if (msisdn != null && !msisdn.trim().isEmpty()) {
            
            BigDecimal balance = DatabaseManager.queryBalance(msisdn);
            resp.getWriter().write(balance.toString());
            //need to  implememnt handling for null reterning 

        }else { 
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("{\"error\": \"User not found\"}");

        }

    }
}
