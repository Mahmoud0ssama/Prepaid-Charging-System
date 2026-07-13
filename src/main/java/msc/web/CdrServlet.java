package msc.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import msc.DatabaseManager;
import msc.DatabaseManager.CDR;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class CdrServlet extends HttpServlet {
        private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("[CdrServlet] doGet called");
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        try {
            List<CDR> cdrs = DatabaseManager.getAllCDRs();
            StringBuilder sb = new StringBuilder();
            sb.append('[');
            boolean first = true;
            for (CDR c : cdrs) {
                if (!first) sb.append(',');
                first = false;
                sb.append('{');
                sb.append("\"msisdn\":\"").append(escapeJson(c.msisdn)).append("\"");
                sb.append(',');
                sb.append("\"startTime\":");
                sb.append(c.startTime == null ? "null" : ("\"" + escapeJson(c.startTime.toString()) + "\""));
                sb.append(',');
                sb.append("\"endTime\":");
                sb.append(c.endTime == null ? "null" : ("\"" + escapeJson(c.endTime.toString()) + "\""));
                sb.append(',');
                sb.append("\"duration\":").append(c.duration);
                sb.append(',');
                sb.append("\"callResult\":\"").append(escapeJson(c.callResult)).append("\"");
                sb.append(',');
                sb.append("\"callCost\":").append(c.callCost == null ? "null" : c.callCost.toString());
                sb.append(',');
                sb.append("\"balanceAfterCall\":").append(c.balanceAfterCall == null ? "null" : c.balanceAfterCall.toString());
                sb.append('}');
            }
            sb.append(']');
            resp.getWriter().write(sb.toString());
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return null;
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
