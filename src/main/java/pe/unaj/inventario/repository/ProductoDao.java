package pe.unaj.inventario.repository;

import pe.unaj.inventario.model.Producto;
import java.io.IOException;
import java.util.List;

public interface ProductoDao {
    List<Producto> cargar() throws IOException;
    void guardar(List<Producto> productos) throws IOException;
}