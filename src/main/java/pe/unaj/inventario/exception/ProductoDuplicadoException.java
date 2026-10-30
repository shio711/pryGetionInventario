package pe.unaj.inventario.exception;

public class ProductoDuplicadoException extends IllegalArgumentException {
    public ProductoDuplicadoException(String codigo) {
        super("Ya existe un producto con el código " + codigo + ".");
    }
}