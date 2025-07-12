package co.hotwax.util;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        this.headers = headers;
        this.rowLimit = rowLimit;
    }

    private void createCSVFile() throws IOException {
        String currentFilePath = fileIndex == 1 ? filePath : (filePath.endsWith(".csv") ? filePath.replace(".csv", "_" + fileIndex + ".csv") : filePath + "_" + fileIndex + ".csv");
        File file = new File(currentFilePath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        this.writer = Files.newBufferedWriter(Paths.get(currentFilePath), StandardCharsets.UTF_8);
        this.csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(headers.toArray(new String[0])).withRecordSeparator("\n").withIgnoreEmptyLines(true));

        generatedFilePaths.add(currentFilePath);
    }

    public void writeToFile(Map rowData) throws IOException {
        if (csvPrinter == null) {
            if (this.headers == null) {this.headers = new ArrayList<>(rowData.keySet());}
            createCSVFile();
        }
        List<Object> record = new ArrayList<>();
        for (String header : headers) {
            Object value = rowData.get(header);
            record.add(value != null ? value : "");
        }
        csvPrinter.printRecord(record);
        rowCount++;

        if (rowLimit > 0 && rowCount >= rowLimit) {
            close();
            fileIndex++;
            rowCount = 0;
            createCSVFile();
        }
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
        return generatedFilePaths;
    }
}