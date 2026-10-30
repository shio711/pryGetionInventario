package pe.unaj.inventario.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pe.unaj.inventario.exception.ProductoDuplicadoException;
import pe.unaj.inventario.exception.StockInsuficienteException;
import pe.unaj.inventario.model.Movimiento;
import pe.unaj.inventario.model.Producto;
import pe.unaj.inventario.repository.ArchivoProductoDao;
import pe.unaj.inventario.repository.CsvMovimientoDao;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GestorInventarioTest {
    @TempDir
    Path carpetaTemporal;

    private GestorInventario nuevoGestor() {
        return new GestorInventario(
                new ArchivoProductoDao(carpetaTemporal.resolve("productos.txt")),
                new CsvMovimientoDao(carpetaTemporal.resolve("movimientos.csv")));
    }

    private Producto producto(String codigo, double stock, double precio) {
        return new Producto(codigo, "Producto " + codigo, "Abarrotes", "unidad", stock, 2, precio);
    }

    @Test
    void registraProductoYLoPersiste() throws IOException {
        GestorInventario gestor = nuevoGestor();
        gestor.agregarProducto(producto("P001", 10, 3.5));

        GestorInventario recargado = nuevoGestor();
        recargado.cargarDatos();

        assertEquals(1, recargado.listarProductos().size());
        assertEquals(10, recargado.buscarPorCodigo("p001").getExistencias());
    }

    @Test
    void rechazaCodigosDuplicadosIgnorandoMayusculas() throws IOException {
        GestorInventario gestor = nuevoGestor();
        gestor.agregarProducto(producto("P001", 10, 3.5));

        assertThrows(ProductoDuplicadoException.class, () -> gestor.agregarProducto(producto("p001", 4, 2)));
        assertEquals(1, gestor.listarProductos().size());
    }

    @Test
    void rechazaPrecioNegativoEnElModelo() {
        assertThrows(IllegalArgumentException.class, () -> producto("P001", 10, -0.01));
    }

    @Test
    void rechazaPrecioNegativoDesdeElServicio() {
        GestorInventario gestor = nuevoGestor();

        assertThrows(IllegalArgumentException.class, () -> gestor.agregarProducto(
                "P001", "Producto P001", "Abarrotes", "unidad", 10, 2, -0.01));
        assertTrue(gestor.listarProductos().isEmpty());
    }
    @Test
    void rechazaCantidadNegativaYNoModificaExistencias() throws IOException {
        GestorInventario gestor = nuevoGestor();
        gestor.agregarProducto(producto("P001", 10, 3.5));

        assertThrows(IllegalArgumentException.class, () -> gestor.registrarEntrada("P001", -1));
        assertEquals(10, gestor.buscarPorCodigo("P001").getExistencias());
        assertTrue(gestor.listarMovimientos().isEmpty());
    }

    @Test
    void rechazaSalidaMayorQueStockSinCrearMovimiento() throws IOException {
        GestorInventario gestor = nuevoGestor();
        gestor.agregarProducto(producto("P001", 3, 3.5));

        assertThrows(StockInsuficienteException.class, () -> gestor.registrarSalida("P001", 4));
        assertEquals(3, gestor.buscarPorCodigo("P001").getExistencias());
        assertTrue(gestor.listarMovimientos().isEmpty());
    }

    @Test
    void registraMovimientoActualizaStockYSeRecuperaAlCargar() throws IOException {
        GestorInventario gestor = nuevoGestor();
        gestor.agregarProducto(producto("P001", 10, 3.5));
        Movimiento movimiento = gestor.registrarSalida("P001", 2.5);

        assertEquals("MOV-000001", movimiento.getId());
        assertEquals(7.5, gestor.buscarPorCodigo("P001").getExistencias());
        GestorInventario recargado = nuevoGestor();
        recargado.cargarDatos();
        assertEquals(7.5, recargado.buscarPorCodigo("P001").getExistencias());
        assertEquals(1, recargado.listarMovimientos().size());
        assertEquals(2.5, recargado.listarMovimientos().get(0).getCantidad());
    }

    @Test
    void importaElFormatoAnteriorDelHistorial() throws IOException {
        GestorInventario gestor = nuevoGestor();
        gestor.agregarProducto(producto("P001", 10, 3.5));
        Path archivo = carpetaTemporal.resolve("movimientos.csv");
        java.nio.file.Files.writeString(archivo,
                "fechaHora;tipo;codigo;producto;cantidad\n29/10/2026 13:08;Salida;P001;Producto P001;2.00\n");

        GestorInventario recargado = nuevoGestor();
        recargado.cargarDatos();

        assertEquals(1, recargado.listarMovimientos().size());
        assertEquals("Salida", recargado.listarMovimientos().get(0).getTipo());
    }
}