package algo.vetplusapp;

import algo.vetplusapp.db.Row;
import algo.vetplusapp.repo.CajaRepo;
import algo.vetplusapp.repo.ComprasRepo;
import algo.vetplusapp.repo.ReporteRepo;
import algo.vetplusapp.util.Config;
import java.util.ArrayList;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.ToggleButton;

public class AdminShellController extends ShellBase {

    @FXML
    private ToggleButton btnMenuResumen;
    @FXML
    private ToggleButton btnMenuClientes;
    @FXML
    private ToggleButton btnMenuAgenda;
    @FXML
    private ToggleButton btnMenuServicios;
    @FXML
    private ToggleButton btnMenuCaja;
    @FXML
    private ToggleButton btnMenuReportes;
    @FXML
    private ToggleButton btnMenuEmpleados;
    @FXML
    private ToggleButton btnMenuCompras;

    @FXML
    private void initialize() {
        registrar("resumen", "Resumen", btnMenuResumen);
        registrar("clientes", "Clientes y mascotas", btnMenuClientes);
        registrarSecundaria("expediente-mascota", "Clientes y mascotas / Expediente", btnMenuClientes);
        registrar("agenda", "Agenda", btnMenuAgenda);
        registrar("servicios", "Servicios y tarifas", btnMenuServicios);
        registrar("caja", "Caja y pagos", btnMenuCaja);
        registrar("reportes", "Reportes", btnMenuReportes);
        registrar("empleados", "Empleados", btnMenuEmpleados);
        registrar("compras", "Compras e insumos", btnMenuCompras);
        iniciarMarco("resumen");
    }

    @Override
    protected List<String> avisos() {
        List<String> l = new ArrayList<>();
        List<Row> bajos = ComprasRepo.bajoMinimo();
        if (!bajos.isEmpty()) {
            l.add(bajos.size() + (bajos.size() == 1 ? " insumo bajo el mínimo" : " insumos bajo el mínimo"));
        }
        int pendientes = CajaRepo.pendientes().size();
        if (pendientes > 0) {
            l.add(pendientes + (pendientes == 1 ? " cuenta pendiente de cobro" : " cuentas pendientes de cobro"));
        }
        int salidas = ReporteRepo.salidasHoy(Config.hoy());
        if (salidas > 0) {
            l.add(salidas + (salidas == 1 ? " salida de pensión hoy" : " salidas de pensión hoy"));
        }
        return l;
    }
}
