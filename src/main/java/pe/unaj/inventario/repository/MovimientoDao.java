package pe.unaj.inventario.repository;

import pe.unaj.inventario.model.Movimiento;
import pe.unaj.inventario.model.Producto;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface MovimientoDao {
    List<Movimiento> cargar(Map<String, Producto> productosPorCodigo) throws IOException;
    void guardar(List<Movimiento> movimientos) throws IOException;
}