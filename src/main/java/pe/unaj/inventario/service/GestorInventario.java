package pe.unaj.inventario.service;

import pe.unaj.inventario.exception.ProductoDuplicadoException;
import pe.unaj.inventario.model.Movimiento;
import pe.unaj.inventario.model.Producto;
import pe.unaj.inventario.repository.MovimientoDao;
import pe.unaj.inventario.repository.ProductoDao;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.TreeMap;

public class GestorInventario {
    private final TreeMap<String, Producto> productos = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final List<Movimiento> movimientos = new ArrayList<>();
    private final ProductoDao productoDao;
    private final MovimientoDao movimientoDao;
    private final Clock reloj;

    public GestorInventario(ProductoDao productoDao, MovimientoDao movimientoDao) {
        this(productoDao, movimientoDao, Clock.systemDefaultZone());
    }

    GestorInventario(ProductoDao productoDao, MovimientoDao movimientoDao, Clock reloj) {
        this.productoDao = productoDao;
        this.movimientoDao = movimientoDao;
        this.reloj = reloj;
    }

    public void cargarDatos() throws IOException {
        productos.clear();
        movimientos.clear();
        for (Producto producto : productoDao.cargar()) {
            if (productos.containsKey(producto.getCodigo())) throw new ProductoDuplicadoException(producto.getCodigo());
            productos.put(producto.getCodigo(), producto);
        }
        movimientos.addAll(movimientoDao.cargar(productos));
    }

    public Producto agregarProducto(String codigo, String nombre, String categoria, String unidad,
                                    double existencias, double stockMinimo, double precioUnitario) throws IOException {
        Producto producto = new Producto(codigo, nombre, categoria, unidad, existencias, stockMinimo, precioUnitario);
        agregarProducto(producto);
        return producto;
    }
    public void agregarProducto(Producto producto) throws IOException {
        if (producto == null) throw new IllegalArgumentException("El producto es obligatorio.");
        validarProducto(producto);
        if (productos.containsKey(producto.getCodigo())) throw new ProductoDuplicadoException(producto.getCodigo());
        productos.put(producto.getCodigo(), producto);
        try {
            productoDao.guardar(listarProductos());
        } catch (IOException ex) {
            productos.remove(producto.getCodigo());
            throw ex;
        }
    }

    public Producto buscarPorCodigo(String codigo) {
        return codigo == null ? null : productos.get(codigo.trim());
    }

    public List<Producto> buscar(String texto) {
        String consulta = texto == null ? "" : texto.trim().toLowerCase();
        return productos.values().stream()
                .filter(p -> p.getCodigo().toLowerCase().contains(consulta)
                        || p.getNombre().toLowerCase().contains(consulta)
                        || p.getCategoria().toLowerCase().contains(consulta))
                .toList();
    }

    public List<Producto> listarProductos() {
        return Collections.unmodifiableList(new ArrayList<>(productos.values()));
    }

    public List<Producto> listarStockBajo() {
        PriorityQueue<Producto> prioridad = new PriorityQueue<>(Comparator
                .comparingDouble((Producto p) -> p.getExistencias() - p.getStockMinimo())
                .thenComparing(Producto::getNombre, String.CASE_INSENSITIVE_ORDER));
        productos.values().stream().filter(Producto::isStockBajo).forEach(prioridad::add);
        List<Producto> resultado = new ArrayList<>();
        while (!prioridad.isEmpty()) resultado.add(prioridad.remove());
        return resultado;
    }

    public double totalUnidadesDisponibles() {
        return productos.values().stream().mapToDouble(Producto::getExistencias).sum();
    }

    public Movimiento registrarEntrada(String codigoProducto, double cantidad) throws IOException {
        return registrarMovimiento(codigoProducto, cantidad, true);
    }

    public Movimiento registrarSalida(String codigoProducto, double cantidad) throws IOException {
        return registrarMovimiento(codigoProducto, cantidad, false);
    }

    private Movimiento registrarMovimiento(String codigo, double cantidad, boolean entrada) throws IOException {
        if (!Double.isFinite(cantidad) || cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser un número mayor que cero.");
        }
        Producto producto = buscarPorCodigo(codigo);
        if (producto == null) throw new IllegalArgumentException("No se encontró el producto indicado.");
        if (entrada) producto.registrarEntrada(cantidad);
        else producto.registrarVenta(cantidad);

        String id = siguienteIdMovimiento();
        Movimiento movimiento = new Movimiento(id, entrada ? "Entrada" : "Salida",
                LocalDateTime.now(reloj), cantidad, producto);
        movimientos.add(movimiento);
        try {
            productoDao.guardar(listarProductos());
            movimientoDao.guardar(listarMovimientos());
        } catch (IOException ex) {
            movimientos.remove(movimiento);
            if (entrada) producto.registrarVenta(cantidad);
            else producto.registrarEntrada(cantidad);
            try {
                productoDao.guardar(listarProductos());
            } catch (IOException rollbackError) {
                ex.addSuppressed(rollbackError);
            }
            throw ex;
        }
        return movimiento;
    }

    private String siguienteIdMovimiento() {
        int maximo = 0;
        for (Movimiento movimiento : movimientos) {
            String id = movimiento.getId();
            if (id.startsWith("MOV-")) {
                try { maximo = Math.max(maximo, Integer.parseInt(id.substring(4))); }
                catch (NumberFormatException ignored) { /* Los identificadores no numéricos no afectan la secuencia. */ }
            }
        }
        return String.format("MOV-%06d", maximo + 1);
    }
    private static void validarProducto(Producto producto) {
        if (!Double.isFinite(producto.getPrecioUnitario()) || producto.getPrecioUnitario() < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo.");
        }
        if (!Double.isFinite(producto.getExistencias()) || producto.getExistencias() < 0) {
            throw new IllegalArgumentException("Las existencias iniciales no pueden ser negativas.");
        }
        if (!Double.isFinite(producto.getStockMinimo()) || producto.getStockMinimo() < 0) {
            throw new IllegalArgumentException("El stock mínimo no puede ser negativo.");
        }
    }

    public List<Movimiento> listarMovimientos() {
        return Collections.unmodifiableList(new ArrayList<>(movimientos));
    }
}