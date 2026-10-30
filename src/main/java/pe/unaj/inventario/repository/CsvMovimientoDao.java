package pe.unaj.inventario.repository;

import pe.unaj.inventario.model.Movimiento;
import pe.unaj.inventario.model.Producto;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CsvMovimientoDao implements MovimientoDao {
    private static final String ENCABEZADO = "id;tipo;fecha;cantidad;codigo;producto";
    private static final DateTimeFormatter FECHA_ANTERIOR = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final Path ruta;

    public CsvMovimientoDao(Path ruta) { this.ruta = ruta; }

    @Override
    public List<Movimiento> cargar(Map<String, Producto> productosPorCodigo) throws IOException {
        if (!Files.exists(ruta)) return new ArrayList<>();
        List<String> filas = Files.readAllLines(ruta, StandardCharsets.UTF_8);
        List<Movimiento> movimientos = new ArrayList<>();
        boolean formatoNuevo = !filas.isEmpty() && filas.get(0).startsWith("id;");
        for (int i = 1; i < filas.size(); i++) {
            String fila = filas.get(i).trim();
            if (fila.isEmpty()) continue;
            String[] c = fila.split(";", -1);
            try {
                if (formatoNuevo && c.length == 6) {
                    Producto producto = buscarProducto(productosPorCodigo, c[4], i);
                    movimientos.add(new Movimiento(c[0], c[1], LocalDateTime.parse(c[2]),
                            Double.parseDouble(c[3]), producto));
                } else if (!formatoNuevo && c.length == 5) {
                    Producto producto = buscarProducto(productosPorCodigo, c[2], i);
                    LocalDateTime fecha = LocalDateTime.parse(c[0], FECHA_ANTERIOR);
                    movimientos.add(new Movimiento(String.format("MOV-%06d", movimientos.size() + 1), c[1], fecha,
                            Double.parseDouble(c[4]), producto));
                } else {
                    throw new IOException("Fila inválida en movimientos.csv: " + (i + 1));
                }
            } catch (RuntimeException ex) {
                throw new IOException("No se pudo leer movimientos.csv, fila " + (i + 1) + ": " + ex.getMessage(), ex);
            }
        }
        return movimientos;
    }

    private static Producto buscarProducto(Map<String, Producto> productos, String codigo, int fila) throws IOException {
        Producto producto = productos.get(codigo);
        if (producto == null) throw new IOException("El código " + codigo + " de la fila " + (fila + 1) + " no existe en productos.txt.");
        return producto;
    }

    @Override
    public void guardar(List<Movimiento> movimientos) throws IOException {
        List<String> filas = new ArrayList<>();
        filas.add(ENCABEZADO);
        for (Movimiento m : movimientos) {
            filas.add(String.join(";", m.getId(), m.getTipo(), m.getFecha().toString(),
                    Double.toString(m.getCantidad()), m.getProducto().getCodigo(), m.getProducto().getNombre()));
        }
        Path absoluta = ruta.toAbsolutePath();
        Files.createDirectories(absoluta.getParent());
        Files.write(absoluta, filas, StandardCharsets.UTF_8);
    }
}