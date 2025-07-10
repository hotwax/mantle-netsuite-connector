package co.hotwax.util;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CsvWriteHelper implements AutoCloseable {
    private final String filePath;
    private List<String> headers;
    private final int rowLimit;
    private int rowCount = 0;
    private int fileIndex = 1;
    private CSVPrinter csvPrinter;
    private BufferedWriter writer;
    private final List<String> generatedFilePaths = new ArrayList<>();


    public CsvWriteHelper(String filePath, List<String> headers, int rowLimit) {
        this.filePath = filePath;
        this.rowLimit = rowLimit;
        this.headers = headers;
    }

    public CsvWriteHelper(String filePath, int rowLimit) {
        this(filePath, null, rowLimit);
    }

    public CsvWriteHelper(String filePath, List<String> headers) {
        this(filePath, headers, 0);
    }


    public CsvWriteHelper(String filePath) {
        this(filePath, null, 0);
    }


    public void setHeaders(List<String> headers) {
        if (headers != null) {
            this.headers = new ArrayList<>(headers);
        }
    }

    private String getCurrentFilePath() {
        if (fileIndex == 1) {
            return filePath;
        }
        if (filePath.endsWith(".csv")) {
            return filePath.replace(".csv", "_" + fileIndex + ".csv");
        }
        return filePath + "_" + fileIndex + ".csv";
    }

    private void createCSVFile() throws IOException {
        String currentFilePath = getCurrentFilePath();
        File file = new File(currentFilePath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        this.writer = new BufferedWriter(new FileWriter(file));
        this.csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(headers.toArray(new String[0])).withRecordSeparator("\n").withIgnoreEmptyLines(true));

        generatedFilePaths.add(currentFilePath);
    }

    public void writeToFile(List<String> rowData) throws IOException {
        if (csvPrinter == null || (rowLimit > 0 && rowCount >= rowLimit)) {
            close();
            if (rowLimit > 0 && rowCount >= rowLimit) {
                fileIndex++;
                rowCount = 0;
            }
            createCSVFile();
        }
        csvPrinter.printRecord(rowData);
        rowCount++;
    }

    @Override
    public void close() throws IOException {
        if (csvPrinter != null) {
            csvPrinter.flush();
            csvPrinter.close();
            csvPrinter = null;
        }
        if (writer != null) {
            writer.close();
            writer = null;
        }
    }

    public List<String> getGeneratedFilePaths() {
        return new ArrayList<>(generatedFilePaths);
    }
}