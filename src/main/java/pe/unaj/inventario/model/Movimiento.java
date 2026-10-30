package pe.unaj.inventario.model;

import java.time.LocalDateTime;
import java.util.Objects;

public final class Movimiento {
    private final String id;
    private final String tipo;
    private final LocalDateTime fecha;
    private final double cantidad;
    private final Producto producto;

    public Movimiento(String id, String tipo, LocalDateTime fecha, double cantidad, Producto producto) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("El movimiento requiere un identificador.");
        if (tipo == null || tipo.isBlank()) throw new IllegalArgumentException("El tipo de movimiento es obligatorio.");
        if (!Double.isFinite(cantidad) || cantidad <= 0) throw new IllegalArgumentException("La cantidad debe ser mayor que cero.");
        this.id = id.trim();
        this.tipo = tipo.trim();
        this.fecha = Objects.requireNonNull(fecha, "La fecha es obligatoria.");
        this.cantidad = cantidad;
        this.producto = Objects.requireNonNull(producto, "El producto es obligatorio.");
    }

    public String getId() { return id; }
    public String getTipo() { return tipo; }
    public LocalDateTime getFecha() { return fecha; }
    public double getCantidad() { return cantidad; }
    public Producto getProducto() { return producto; }
}