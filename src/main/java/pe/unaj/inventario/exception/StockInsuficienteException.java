package pe.unaj.inventario.exception;

public class StockInsuficienteException extends IllegalArgumentException {
    public StockInsuficienteException(String mensaje) {
        super(mensaje);
    }
}