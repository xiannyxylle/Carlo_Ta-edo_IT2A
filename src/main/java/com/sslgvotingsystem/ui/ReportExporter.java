package com.sslgvotingsystem.ui;

import com.sslgvotingsystem.dao.CandidateDao;
import com.sslgvotingsystem.dao.PositionDao;
import com.sslgvotingsystem.dao.VoteDao;
import com.sslgvotingsystem.model.Candidate;
import com.sslgvotingsystem.model.Position;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportExporter {

    public static class ResultRow {
        public final String positionName;
        public final String candidateName;
        public final String partyList;
        public final int votes;
        public final String remarks;

        public ResultRow(String positionName, String candidateName, String partyList, int votes, String remarks) {
            this.positionName = positionName;
            this.candidateName = candidateName;
            this.partyList = partyList;
            this.votes = votes;
            this.remarks = remarks;
        }
    }

    public static List<ResultRow> buildOfficialSummary(PositionDao positionDao, CandidateDao candidateDao, VoteDao voteDao) {
        List<Position> positions = positionDao.getAllPositions();
        List<Candidate> allCandidates = candidateDao.getAllCandidates();
        List<String[]> rawTally = voteDao.getElectionTally();

        Map<String, Integer> voteLookup = new HashMap<>();
        for (String[] row : rawTally) {
            String key = row[0] + "||" + row[1];
            int count = 0;
            try {
                count = Integer.parseInt(row[3].trim());
            } catch (NumberFormatException ignored) {
            }
            voteLookup.put(key, count);
        }

        List<ResultRow> results = new ArrayList<>();

        for (Position pos : positions) {
            List<Candidate> posCandidates = allCandidates.stream()
                    .filter(c -> c.positionId() == pos.id())
                    .sorted((a, b) -> {
                        int votesB = voteLookup.getOrDefault(pos.positionName() + "||" + b.candidateName(), 0);
                        int votesA = voteLookup.getOrDefault(pos.positionName() + "||" + a.candidateName(), 0);
                        int cmp = Integer.compare(votesB, votesA);
                        return cmp != 0 ? cmp : a.candidateName().compareToIgnoreCase(b.candidateName());
                    })
                    .toList();

            int maxWinners = Math.max(1, pos.maxWinners());
            int cutoffVotes = 0;
            if (!posCandidates.isEmpty()) {
                int cutoffIndex = Math.min(maxWinners, posCandidates.size()) - 1;
                Candidate cutoffCand = posCandidates.get(cutoffIndex);
                cutoffVotes = voteLookup.getOrDefault(pos.positionName() + "||" + cutoffCand.candidateName(), 0);
            }

            for (Candidate c : posCandidates) {
                int votes = voteLookup.getOrDefault(pos.positionName() + "||" + c.candidateName(), 0);
                String remark = (votes > 0 && votes >= cutoffVotes) ? "DECLARED WINNER" : "-";
                results.add(new ResultRow(pos.positionName(), c.candidateName(), c.partyList(), votes, remark));
            }
        }

        return results;
    }

    public static String exportSummaryToCsv(PositionDao positionDao, CandidateDao candidateDao, VoteDao voteDao) {
        int[] stats = voteDao.getVoterTurnoutStats();
        int totalStudents = stats[0];
        int totalVoted = stats[1];
        double percentage = totalStudents > 0 ? (totalVoted * 100.0 / totalStudents) : 0.0;

        List<ResultRow> rows = buildOfficialSummary(positionDao, candidateDao, voteDao);

        String fileTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String displayTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a"));
        Path outputPath = Paths.get("SSLG_Election_Results_" + fileTimestamp + ".csv").toAbsolutePath();

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath.toFile()))) {
            writer.println("ELIGIO T. MONTE DE RAMOS HIGH SCHOOL SSLG ELECTION RESULTS SUMMARY");
            writer.println("Generated On," + escapeCsv(displayTimestamp));
            writer.printf("Voter Turnout,%d / %d Approved Students (%.1f%%)%n", totalVoted, totalStudents, percentage);
            writer.println();
            writer.println("Position,Candidate Name,Party List,Total Votes,Remarks");

            for (ResultRow r : rows) {
                writer.printf("%s,%s,%s,%d,%s%n",
                        escapeCsv(r.positionName),
                        escapeCsv(r.candidateName),
                        escapeCsv(r.partyList),
                        r.votes,
                        escapeCsv(r.remarks));
            }
            return outputPath.toString();
        } catch (IOException e) {
            System.out.println("Error exporting CSV report: " + e.getMessage());
            return null;
        }
    }

    public static String exportSummaryToExcel(PositionDao positionDao, CandidateDao candidateDao, VoteDao voteDao) {
        int[] stats = voteDao.getVoterTurnoutStats();
        int totalStudents = stats[0];
        int totalVoted = stats[1];
        double percentage = totalStudents > 0 ? (totalVoted * 100.0 / totalStudents) : 0.0;

        List<ResultRow> rows = buildOfficialSummary(positionDao, candidateDao, voteDao);

        String fileTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String displayTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a"));
        Path outputPath = Paths.get("SSLG_Election_Results_" + fileTimestamp + ".xls").toAbsolutePath();

        StringBuilder html = new StringBuilder();
        html.append("<html><head><meta charset=\"UTF-8\"></head><body>");
        html.append("<h2>\"ELIGIO T. MONTE DE RAMOS HIGH SCHOOL SSLG ELECTION RESULTS SUMMARY\"</h2>");
        html.append("<p><b>Generated On:</b> ").append(escapeHtml(displayTimestamp)).append("</p>");
        html.append("<p><b>Voter Turnout:</b> ").append(totalVoted).append(" / ").append(totalStudents)
                .append(String.format(" Approved Students (%.1f%%)", percentage)).append("</p>");
        html.append("<table border=\"1\" cellpadding=\"6\" cellspacing=\"0\">");
        html.append("<tr><th>Position</th><th>Candidate Name</th><th>Party List</th><th>Total Votes</th><th>Remarks</th></tr>");

        for (ResultRow r : rows) {
            html.append("<tr>")
                    .append("<td>").append(escapeHtml(r.positionName)).append("</td>")
                    .append("<td>").append(escapeHtml(r.candidateName)).append("</td>")
                    .append("<td>").append(escapeHtml(r.partyList)).append("</td>")
                    .append("<td>").append(r.votes).append("</td>")
                    .append("<td>").append(escapeHtml(r.remarks)).append("</td>")
                    .append("</tr>");
        }

        html.append("</table></body></html>");

        try (FileOutputStream fos = new FileOutputStream(outputPath.toFile())) {
            fos.write(html.toString().getBytes(StandardCharsets.UTF_8));
            return outputPath.toString();
        } catch (IOException e) {
            System.out.println("Error exporting Excel report: " + e.getMessage());
            return null;
        }
    }

    public static String exportSummaryToPdf(PositionDao positionDao, CandidateDao candidateDao, VoteDao voteDao) {
        int[] stats = voteDao.getVoterTurnoutStats();
        int totalStudents = stats[0];
        int totalVoted = stats[1];
        double percentage = totalStudents > 0 ? (totalVoted * 100.0 / totalStudents) : 0.0;

        List<ResultRow> rows = buildOfficialSummary(positionDao, candidateDao, voteDao);

        String fileTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String displayTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a"));
        Path outputPath = Paths.get("SSLG_Election_Results_" + fileTimestamp + ".pdf").toAbsolutePath();

        List<String> lines = new ArrayList<>();
        lines.add("SSLG ELECTION RESULTS SUMMARY");
        lines.add("Generated On: " + displayTimestamp);
        lines.add(String.format("Voter Turnout: %d / %d Approved Students (%.1f%%)", totalVoted, totalStudents, percentage));
        lines.add("--------------------------------------------------------------------------------");
        lines.add(String.format("%-20s %-22s %-15s %-6s %-15s",
                "Position", "Candidate Name", "Party List", "Votes", "Remarks"));
        lines.add("--------------------------------------------------------------------------------");

        for (ResultRow r : rows) {
            lines.add(String.format("%-20s %-22s %-15s %-6d %-15s",
                    truncate(r.positionName, 20),
                    truncate(r.candidateName, 22),
                    truncate(r.partyList, 15),
                    r.votes,
                    truncate(r.remarks, 15)));
        }

        try {
            byte[] pdfBytes = buildSimplePdfBytes(lines);
            try (FileOutputStream fos = new FileOutputStream(outputPath.toFile())) {
                fos.write(pdfBytes);
            }
            return outputPath.toString();
        } catch (IOException e) {
            System.out.println("Error exporting PDF report: " + e.getMessage());
            return null;
        }
    }

    private static byte[] buildSimplePdfBytes(List<String> lines) throws IOException {
        StringBuilder content = new StringBuilder();
        content.append("BT\n/F1 10 Tf\n40 750 Td\n14 TL\n");
        for (String line : lines) {
            content.append("(").append(escapePdfText(line)).append(") Tj T*\n");
        }
        content.append("ET\n");

        byte[] streamBytes = content.toString().getBytes(StandardCharsets.ISO_8859_1);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();

        writeAscii(out, "%PDF-1.4\n");

        offsets.add(out.size());
        writeAscii(out, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

        offsets.add(out.size());
        writeAscii(out, "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");

        offsets.add(out.size());
        writeAscii(out, "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>\nendobj\n");

        offsets.add(out.size());
        writeAscii(out, "4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Courier >>\nendobj\n");

        offsets.add(out.size());
        writeAscii(out, "5 0 obj\n<< /Length " + streamBytes.length + " >>\nstream\n");
        out.write(streamBytes);
        writeAscii(out, "endstream\nendobj\n");

        int xrefOffset = out.size();
        writeAscii(out, "xref\n0 " + (offsets.size() + 1) + "\n");
        writeAscii(out, "0000000000 65535 f \n");
        for (int offset : offsets) {
            writeAscii(out, String.format("%010d 00000 n \n", offset));
        }

        writeAscii(out, "trailer\n<< /Size " + (offsets.size() + 1) + " /Root 1 0 R >>\nstartxref\n" + xrefOffset + "\n%%EOF\n");
        return out.toByteArray();
    }

    private static void writeAscii(ByteArrayOutputStream out, String text) throws IOException {
        out.write(text.getBytes(StandardCharsets.ISO_8859_1));
    }

    private static String escapePdfText(String text) {
        if (text == null) return "";
        String sanitized = text.replaceAll("[^\\x20-\\x7E]", "?");
        return sanitized
                .replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }

    private static String truncate(String value, int maxLen) {
        if (value == null) return "";
        return value.length() <= maxLen ? value : value.substring(0, maxLen - 3) + "...";
    }

    private static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static String escapeHtml(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}