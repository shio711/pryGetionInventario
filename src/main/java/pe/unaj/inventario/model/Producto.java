package pe.unaj.inventario.model;

import pe.unaj.inventario.exception.StockInsuficienteException;

public class Producto {
    private final String codigo;
    private final String nombre;
    private final String categoria;
    private final String unidad;
    private final double stockMinimo;
    private final double precioUnitario;
    private double existencias;

    public Producto(String codigo, String nombre, String categoria, String unidad,
                    double existencias, double stockMinimo, double precioUnitario) {
        this.codigo = requerido(codigo, "Ingrese un código.").toUpperCase();
        this.nombre = requerido(nombre, "Ingrese el nombre del producto.");
        this.categoria = requerido(categoria, "Seleccione una categoría.");
        this.unidad = requerido(unidad, "Seleccione una unidad.");
        validarNoNegativo(existencias, "Las existencias");
        validarNoNegativo(stockMinimo, "El stock mínimo");
        validarNoNegativo(precioUnitario, "El precio");
        this.existencias = existencias;
        this.stockMinimo = stockMinimo;
        this.precioUnitario = precioUnitario;
    }

    private static String requerido(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException(mensaje);
        String limpio = valor.trim();
        if (limpio.contains(";") || limpio.contains("\n") || limpio.contains("\r")) {
            throw new IllegalArgumentException("No use punto y coma ni saltos de línea.");
        }
        return limpio;
    }

    private static void validarNoNegativo(double valor, String campo) {
        if (!Double.isFinite(valor) || valor < 0) {
            throw new IllegalArgumentException(campo + " no pueden ser negativos.");
        }
    }

    private static void validarCantidad(double cantidad) {
        if (!Double.isFinite(cantidad) || cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser un número mayor que cero.");
        }
    }

    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public String getCategoria() { return categoria; }
    public String getUnidad() { return unidad; }
    public double getExistencias() { return existencias; }
    public double getStockMinimo() { return stockMinimo; }
    public double getPrecioUnitario() { return precioUnitario; }
    public boolean isStockBajo() { return existencias <= stockMinimo; }

    public void registrarEntrada(double cantidad) {
        validarCantidad(cantidad);
        double nuevoStock = existencias + cantidad;
        if (!Double.isFinite(nuevoStock)) throw new IllegalArgumentException("La cantidad supera el límite permitido.");
        existencias = nuevoStock;
    }

    public void registrarVenta(double cantidad) {
        validarCantidad(cantidad);
        if (cantidad > existencias) {
            throw new StockInsuficienteException("La cantidad solicitada supera las existencias disponibles.");
        }
        existencias -= cantidad;
    }
}