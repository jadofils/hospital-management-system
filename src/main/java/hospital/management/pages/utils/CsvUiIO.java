package hospital.management.pages.utils;

import hospital.management.backend.service.fileio.FileExportService;
import hospital.management.backend.service.fileio.FileImportService;
import hospital.management.backend.service.fileio.impl.CsvFileExportService;
import hospital.management.backend.service.fileio.impl.CsvFileImportService;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class CsvUiIO {

    private static final FileExportService EXPORTER = new CsvFileExportService();
    private static final FileImportService IMPORTER = new CsvFileImportService();

    private CsvUiIO() {
    }

    public static boolean exportRows(Window owner, String initialFileName, List<Map<String, Object>> rows) throws Exception {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        chooser.setInitialFileName(initialFileName);
        File file = chooser.showSaveDialog(owner);
        if (file == null) {
            return false;
        }
        EXPORTER.exportTo(file.toPath(), rows);
        return true;
    }

    public static List<Map<String, String>> importRows(Window owner, String title) throws Exception {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = chooser.showOpenDialog(owner);
        if (file == null) {
            return List.of();
        }

        List<Map<String, String>> rows = new ArrayList<>();
        IMPORTER.importFile(file.toPath(), rows::add);
        return rows;
    }
}
