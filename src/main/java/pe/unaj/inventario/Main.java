package pe.unaj.inventario;

import pe.unaj.inventario.repository.ArchivoProductoDao;
import pe.unaj.inventario.repository.CsvMovimientoDao;
import pe.unaj.inventario.service.GestorInventario;
import pe.unaj.inventario.ui.FrmPrincipal;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.nio.file.Path;

public final class Main {
    private Main() { }

    public static void main(String[] args) {
        GestorInventario gestor = new GestorInventario(
                new ArchivoProductoDao(Path.of("data", "productos.txt")),
                new CsvMovimientoDao(Path.of("data", "movimientos.csv")));
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // Swing conserva su aspecto predeterminado si el aspecto del sistema no está disponible.
            }
            new FrmPrincipal(gestor).setVisible(true);
        });
    }
}