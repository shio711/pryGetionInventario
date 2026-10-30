package pe.unaj.inventario.ui;

import pe.unaj.inventario.model.Movimiento;
import pe.unaj.inventario.model.Producto;
import pe.unaj.inventario.service.GestorInventario;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class FrmPrincipal extends JFrame {
    private JPanel panelPrincipal;
    private JTabbedPane tabbedPanePrincipal;
    private JPanel panelResumen;
    private JPanel panelProductos;
    private JLabel lblResumen;
    private JTable tablaProductos;
    private JTable tablaAlertas;
    private JTextField txtBuscar;
    private JButton btnAgregarProducto;
    private JPanel panelMovimientos;
    private JTextField txtCodigo;
    private JTextField txtCantidad;
    private JButton btnEntrada;
    private JButton btnVenta;
    private JTable tablaMovimientos;
    private JPanel panelHistorial;

    private final GestorInventario gestor;
    private final DefaultTableModel modeloProductos = modelo("Código", "Producto", "Categoría", "Unidad", "Existencias", "Stock mínimo", "Precio unitario (S/)");
    private final DefaultTableModel modeloAlertas = modelo("Código", "Producto", "Existencias", "Stock mínimo", "Estado");
    private final DefaultTableModel modeloMovimientos = modelo("ID", "Fecha y hora", "Movimiento", "Código", "Producto", "Cantidad");
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public FrmPrincipal(GestorInventario gestor) {
        this.gestor = gestor;
        setTitle("pryGestionInventario | Gestión de inventario");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setContentPane(panelPrincipal);
        setMinimumSize(new Dimension(980, 650));
        setSize(1180, 760);
        setLocationRelativeTo(null);
        prepararInterfaz();
        try {
            gestor.cargarDatos();
        } catch (IOException | RuntimeException ex) {
            mostrarError("No se pudieron cargar los datos: " + ex.getMessage());
        }
        refrescar();
    }

    private void prepararInterfaz() {
        prepararTabla(tablaProductos, modeloProductos);
        prepararTabla(tablaAlertas, modeloAlertas);
        prepararTabla(tablaMovimientos, modeloMovimientos);
        txtBuscar.setToolTipText("Escribe código, nombre o categoría para filtrar el catálogo");
        txtBuscar.setBorder(BorderFactory.createTitledBorder("Buscar en el catálogo"));
        txtCodigo.setToolTipText("Escribe el código del producto o selecciónalo con doble clic en el catálogo");
        txtCodigo.setBorder(BorderFactory.createTitledBorder("Código del producto"));
        txtCantidad.setToolTipText("Cantidad en la unidad indicada para el producto");
        txtCantidad.setBorder(BorderFactory.createTitledBorder("Cantidad"));
        ((JScrollPane) tablaProductos.getParent().getParent()).setBorder(BorderFactory.createTitledBorder("Productos registrados"));
        ((JScrollPane) tablaAlertas.getParent().getParent()).setBorder(BorderFactory.createTitledBorder("Productos que requieren reposición"));
        ((JScrollPane) tablaMovimientos.getParent().getParent()).setBorder(BorderFactory.createTitledBorder("Entradas y salidas registradas"));
        btnAgregarProducto.setText("+ Agregar producto");
        btnEntrada.setText("Registrar entrada");
        btnVenta.setText("Registrar salida");
        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            private void filtrar() { llenarProductos(gestor.buscar(txtBuscar.getText())); }
            @Override public void insertUpdate(DocumentEvent e) { filtrar(); }
            @Override public void removeUpdate(DocumentEvent e) { filtrar(); }
            @Override public void changedUpdate(DocumentEvent e) { filtrar(); }
        });
        btnAgregarProducto.addActionListener(e -> abrirAltaProducto());
        btnEntrada.addActionListener(e -> registrarMovimiento(true));
        btnVenta.addActionListener(e -> registrarMovimiento(false));
        tablaProductos.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) seleccionarProductoParaMovimiento();
            }
        });
    }

    private void abrirAltaProducto() {
        JTextField codigo = new JTextField();
        JTextField nombre = new JTextField();
        JTextField existencias = new JTextField("0");
        JTextField minimo = new JTextField("0");
        JTextField precio = new JTextField("0.00");
        JComboBox<String> categoria = new JComboBox<>(new String[]{"Abarrotes", "Bebidas", "Limpieza", "Hogar", "Otros"});
        JComboBox<String> unidad = new JComboBox<>(new String[]{"unidad", "kg", "litro", "paquete", "caja"});
        JPanel formulario = new JPanel(new GridLayout(7, 2, 8, 8));
        formulario.add(new JLabel("Código")); formulario.add(codigo);
        formulario.add(new JLabel("Nombre del producto")); formulario.add(nombre);
        formulario.add(new JLabel("Categoría")); formulario.add(categoria);
        formulario.add(new JLabel("Unidad de medida")); formulario.add(unidad);
        formulario.add(new JLabel("Existencias iniciales")); formulario.add(existencias);
        formulario.add(new JLabel("Stock mínimo")); formulario.add(minimo);
        formulario.add(new JLabel("Precio unitario (S/)")); formulario.add(precio);
        int opcion = JOptionPane.showConfirmDialog(this, formulario, "Agregar producto al catálogo", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opcion != JOptionPane.OK_OPTION) return;
        try {
            gestor.agregarProducto(codigo.getText(), nombre.getText(), (String) categoria.getSelectedItem(),
                    (String) unidad.getSelectedItem(), numero(existencias), numero(minimo), numero(precio));
            refrescar();
            tabbedPanePrincipal.setSelectedComponent(panelProductos);
            JOptionPane.showMessageDialog(this, "Producto agregado correctamente.", "Catálogo", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException | IllegalArgumentException ex) {
            mostrarError(ex.getMessage());
        }
    }

    private void seleccionarProductoParaMovimiento() {
        int filaVista = tablaProductos.getSelectedRow();
        if (filaVista < 0) return;
        int fila = tablaProductos.convertRowIndexToModel(filaVista);
        txtCodigo.setText(String.valueOf(modeloProductos.getValueAt(fila, 0)));
        txtCantidad.requestFocusInWindow();
        tabbedPanePrincipal.setSelectedComponent(panelMovimientos);
    }

    private void registrarMovimiento(boolean esEntrada) {
        try {
            double cantidad = numero(txtCantidad);
            if (esEntrada) gestor.registrarEntrada(txtCodigo.getText(), cantidad);
            else gestor.registrarSalida(txtCodigo.getText(), cantidad);
            refrescar();
            txtCantidad.setText("");
            tabbedPanePrincipal.setSelectedComponent(panelHistorial);
        } catch (IOException | IllegalArgumentException ex) {
            mostrarError(ex.getMessage());
        }
    }

    private void refrescar() {
        lblResumen.setText("Productos: " + gestor.listarProductos().size()
                + "   |   Existencias disponibles: " + formato(gestor.totalUnidadesDisponibles())
                + "   |   Productos con stock bajo: " + gestor.listarStockBajo().size());
        modeloAlertas.setRowCount(0);
        for (Producto producto : gestor.listarStockBajo()) {
            modeloAlertas.addRow(new Object[]{producto.getCodigo(), producto.getNombre(), formato(producto.getExistencias()),
                    formato(producto.getStockMinimo()), "Reponer"});
        }
        llenarProductos(gestor.buscar(txtBuscar.getText()));
        modeloMovimientos.setRowCount(0);
        for (Movimiento movimiento : gestor.listarMovimientos()) {
            modeloMovimientos.addRow(new Object[]{movimiento.getId(), movimiento.getFecha().format(FORMATO_FECHA),
                    movimiento.getTipo(), movimiento.getProducto().getCodigo(), movimiento.getProducto().getNombre(),
                    formato(movimiento.getCantidad())});
        }
    }

    private void llenarProductos(List<Producto> productos) {
        modeloProductos.setRowCount(0);
        for (Producto producto : productos) {
            modeloProductos.addRow(new Object[]{producto.getCodigo(), producto.getNombre(), producto.getCategoria(),
                    producto.getUnidad(), formato(producto.getExistencias()), formato(producto.getStockMinimo()),
                    formato(producto.getPrecioUnitario())});
        }
    }

    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Gestión de inventario", JOptionPane.WARNING_MESSAGE);
    }

    private static double numero(JTextField campo) { return Double.parseDouble(campo.getText().trim()); }

    private static DefaultTableModel modelo(String... columnas) {
        return new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int fila, int columna) { return false; }
        };
    }

    private static void prepararTabla(JTable tabla, DefaultTableModel modelo) {
        tabla.setModel(modelo);
        tabla.setRowHeight(27);
        tabla.setAutoCreateRowSorter(true);
        tabla.setFillsViewportHeight(true);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setGridColor(new Color(225, 230, 236));
    }

    private static String formato(double numero) { return String.format(java.util.Locale.ROOT, "%.2f", numero); }
}