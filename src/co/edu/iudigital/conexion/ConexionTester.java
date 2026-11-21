package co.edu.iudigital.conexion;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

/**
 * ConexionTester: aplicación de escritorio (Swing) que permite escribir los datos de una
 * base de datos y comprobar si la conexión JDBC funciona.
 *
 * Organización (programación orientada a objetos):
 *  - La clase ES la ventana (hereda de JFrame) y es dueña de sus componentes, que son privados
 *    (encapsulamiento): nadie fuera de la clase puede cambiarlos.
 *  - La construcción de la interfaz está separada de la lógica de conexión.
 *  - La lógica de conexión está en {@link #probarConexion}, un método estático que no toca la
 *    interfaz: recibe datos y devuelve un {@link Resultado}. Así se puede probar sin ventana.
 *  - El evento del botón lo atiende una clase interna que implementa ActionListener.
 */
public final class ConexionTester extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final Color VERDE = new Color(0, 128, 0);
    private static final Color ROJO = new Color(192, 0, 0);
    private static final Color GRIS = new Color(80, 80, 80);

    private static final String URL_EJEMPLO = "jdbc:sqlite:inventario.db";
    private static final int TIEMPO_ESPERA_SEGUNDOS = 5;

    // Componentes de la interfaz (privados: encapsulamiento)
    private final JTextField campoUrl = new JTextField(URL_EJEMPLO);
    private final JTextField campoUsuario = new JTextField();
    private final JPasswordField campoClave = new JPasswordField();
    private final JButton botonProbar = new JButton("Probar conexión");
    private final JTextArea areaResultado = new JTextArea();

    /** Resultado inmutable de una prueba de conexión: si funcionó y el mensaje para mostrar. */
    record Resultado(boolean exitoso, String mensaje) { }

    // ------------------------------------------------------------------
    // 1. Interfaz gráfica (JFrame, JPanel, layouts, componentes)
    // ------------------------------------------------------------------

    public ConexionTester() {
        super("ConexionTester - Prueba de conexión JDBC");
        configurarVentana();
        setContentPane(construirPanelPrincipal());
        registrarEventos();
        mostrarMensajeInicial();
    }

    private void configurarVentana() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(680, 440);
        setMinimumSize(new Dimension(560, 380));
        setLocationRelativeTo(null); // centrada en la pantalla
    }

    /** Panel principal con BorderLayout: formulario arriba y resultado en el centro. */
    private JPanel construirPanelPrincipal() {
        JPanel principal = new JPanel(new BorderLayout(0, 12));
        principal.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        principal.add(construirPanelSuperior(), BorderLayout.NORTH);
        principal.add(construirPanelResultado(), BorderLayout.CENTER);
        return principal;
    }

    /** Formulario (GridLayout de 3 filas x 2 columnas) y botón. */
    private JPanel construirPanelSuperior() {
        JPanel formulario = new JPanel(new GridLayout(3, 2, 10, 10));
        formulario.add(new JLabel("URL de conexión:"));
        formulario.add(campoUrl);
        formulario.add(new JLabel("Nombre de usuario (vacío si es SQLite):"));
        formulario.add(campoUsuario);
        formulario.add(new JLabel("Contraseña (vacía si es SQLite):"));
        formulario.add(campoClave);

        campoUrl.setToolTipText("Ejemplo: " + URL_EJEMPLO);

        JPanel panelBoton = new JPanel(new BorderLayout());
        botonProbar.setPreferredSize(new Dimension(180, 34));
        panelBoton.add(botonProbar, BorderLayout.EAST);

        JPanel superior = new JPanel(new BorderLayout(0, 12));
        superior.add(formulario, BorderLayout.CENTER);
        superior.add(panelBoton, BorderLayout.SOUTH);
        return superior;
    }

    /** JTextArea dentro de un JScrollPane para mostrar el resultado de la prueba. */
    private JScrollPane construirPanelResultado() {
        areaResultado.setEditable(false);
        areaResultado.setLineWrap(true);
        areaResultado.setWrapStyleWord(true);
        areaResultado.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        areaResultado.setMargin(new java.awt.Insets(8, 10, 8, 10));

        JScrollPane desplazable = new JScrollPane(areaResultado);
        desplazable.setBorder(BorderFactory.createTitledBorder("Resultado de la prueba"));
        return desplazable;
    }

    private void registrarEventos() {
        botonProbar.addActionListener(new ManejadorProbarConexion());
        getRootPane().setDefaultButton(botonProbar); // Enter también prueba la conexión
    }

    private void mostrarMensajeInicial() {
        areaResultado.setForeground(GRIS);
        areaResultado.setText("Escriba los datos de conexión y pulse \"Probar conexión\".");
    }

    // ------------------------------------------------------------------
    // 2. Evento del botón (ActionListener)
    // ------------------------------------------------------------------

    /** Atiende el clic en "Probar conexión": lee los campos, prueba y muestra el resultado. */
    private class ManejadorProbarConexion implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent evento) {
            String url = campoUrl.getText().trim();
            String usuario = campoUsuario.getText().trim();

            // getPassword() devuelve char[]: se borra de memoria apenas se usa.
            char[] clave = campoClave.getPassword();
            Resultado resultado;
            try {
                resultado = probarConexion(url, usuario, new String(clave));
            } finally {
                Arrays.fill(clave, '\0');
            }
            mostrarResultado(resultado);
        }
    }

    /** Verde si la conexión funcionó; rojo si falló. */
    private void mostrarResultado(Resultado resultado) {
        areaResultado.setForeground(resultado.exitoso() ? VERDE : ROJO);
        areaResultado.setText(resultado.mensaje());
        areaResultado.setCaretPosition(0);
    }

    // ------------------------------------------------------------------
    // 3. Lógica de conexión JDBC (DriverManager, excepciones, cierre seguro)
    // ------------------------------------------------------------------

    /**
     * Intenta abrir una conexión con DriverManager y devuelve el resultado.
     * No depende de la interfaz gráfica, por eso se puede probar por separado.
     */
    static Resultado probarConexion(String url, String usuario, String clave) {
        if (url == null || url.isBlank()) {
            return new Resultado(false,
                    "ENTRADA NO VÁLIDA\n\nEscriba la URL de conexión, por ejemplo: " + URL_EJEMPLO);
        }

        DriverManager.setLoginTimeout(TIEMPO_ESPERA_SEGUNDOS);
        String claseDriver = resolverDriver(url);

        try {
            if (claseDriver != null) {
                Class.forName(claseDriver); // lanza ClassNotFoundException si falta el .jar del driver
            }

            // try-with-resources: la conexión se cierra sola, tanto si todo sale bien como si hay error.
            try (Connection conexion = DriverManager.getConnection(url, usuario, clave)) {
                DatabaseMetaData datos = conexion.getMetaData();
                return new Resultado(true, "CONEXIÓN EXITOSA\n\n"
                        + "URL:            " + url + "\n"
                        + "Base de datos:  " + datos.getDatabaseProductName()
                        + " " + datos.getDatabaseProductVersion() + "\n"
                        + "Driver JDBC:    " + datos.getDriverName()
                        + " " + datos.getDriverVersion() + "\n\n"
                        + "La conexión se cerró automáticamente (try-with-resources).");
            }

        } catch (ClassNotFoundException e) {
            return new Resultado(false, "FALLO DEL DRIVER\n\n"
                    + "No se encontró la clase del driver: " + e.getMessage() + "\n"
                    + "Agregue el archivo .jar del driver al classpath (carpeta lib) y vuelva a intentarlo.");

        } catch (SQLException e) {
            return new Resultado(false, "FALLO DE CONEXIÓN\n\n"
                    + "Mensaje técnico: " + e.getMessage() + "\n"
                    + "SQLState: " + (e.getSQLState() == null ? "no disponible" : e.getSQLState()) + "\n"
                    + "Código de error: " + e.getErrorCode());
        }
    }

    /** Devuelve el nombre de la clase del driver según el tipo de URL, o null si no se reconoce. */
    static String resolverDriver(String url) {
        if (url.startsWith("jdbc:sqlite:")) {
            return "org.sqlite.JDBC";
        }
        if (url.startsWith("jdbc:mysql:")) {
            return "com.mysql.cj.jdbc.Driver";
        }
        if (url.startsWith("jdbc:postgresql:")) {
            return "org.postgresql.Driver";
        }
        if (url.startsWith("jdbc:h2:")) {
            return "org.h2.Driver";
        }
        return null; // DriverManager responderá con SQLException ("No suitable driver")
    }

    // ------------------------------------------------------------------
    // Punto de entrada
    // ------------------------------------------------------------------

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Si no se puede usar el aspecto del sistema, Swing usa el suyo por defecto.
        }
        SwingUtilities.invokeLater(() -> new ConexionTester().setVisible(true));
    }
}
