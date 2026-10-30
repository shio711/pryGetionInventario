package pe.unaj.inventario.repository;

import pe.unaj.inventario.model.Producto;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ArchivoProductoDao implements ProductoDao {
    private static final String ENCABEZADO = "codigo;nombre;categoria;unidad;existencias;stockMinimo;precioUnitario";
    private final Path ruta;

    public ArchivoProductoDao(Path ruta) { this.ruta = ruta; }

    @Override
    public List<Producto> cargar() throws IOException {
        if (!Files.exists(ruta)) return new ArrayList<>();
        List<String> filas = Files.readAllLines(ruta, StandardCharsets.UTF_8);
        List<Producto> productos = new ArrayList<>();
        for (int i = 1; i < filas.size(); i++) {
            String fila = filas.get(i).trim();
            if (fila.isEmpty()) continue;
            String[] c = fila.split(";", -1);
            if (c.length != 7) throw new IOException("Fila inválida en productos.txt: " + (i + 1));
            try {
                productos.add(new Producto(c[0], c[1], c[2], c[3], Double.parseDouble(c[4]),
                        Double.parseDouble(c[5]), Double.parseDouble(c[6])));
            } catch (RuntimeException ex) {
                throw new IOException("No se pudo leer productos.txt, fila " + (i + 1) + ": " + ex.getMessage(), ex);
            }
        }
        return productos;
    }

    @Override
    public void guardar(List<Producto> productos) throws IOException {
        List<String> filas = new ArrayList<>();
        filas.add(ENCABEZADO);
        for (Producto p : productos) {
            filas.add(String.join(";", p.getCodigo(), p.getNombre(), p.getCategoria(), p.getUnidad(),
                    Double.toString(p.getExistencias()), Double.toString(p.getStockMinimo()),
                    Double.toString(p.getPrecioUnitario())));
        }
        Path absoluta = ruta.toAbsolutePath();
        Files.createDirectories(absoluta.getParent());
        Files.write(absoluta, filas, StandardCharsets.UTF_8);
    }
}