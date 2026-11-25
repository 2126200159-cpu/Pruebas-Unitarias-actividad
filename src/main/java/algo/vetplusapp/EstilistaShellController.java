package algo.vetplusapp;

import algo.vetplusapp.db.Row;
import algo.vetplusapp.repo.AtencionRepo;
import algo.vetplusapp.util.Config;
import algo.vetplusapp.util.Sesion;
import java.util.ArrayList;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.ToggleButton;

public class EstilistaShellController extends ShellBase {

    @FXML
    private ToggleButton btnMenuEstetica;
    @FXML
    private ToggleButton btnMenuPacientes;

    @FXML
    private void initialize() {
        registrar("estetica-canina", "Estética canina", btnMenuEstetica);
        registrar("mis-pacientes", "Mis pacientes", btnMenuPacientes);
        iniciarMarco("estetica-canina");
    }

    @Override
    protected List<String> avisos() {
        List<String> l = new ArrayList<>();
        int listos = 0;
        int porAtender = 0;
        for (Row c : AtencionRepo.esteticaDelDia(Sesion.usuarioId, Config.hoy())) {
            if ("Finalizada".equals(c.str("estado"))) {
                listos++;
            } else if ("Confirmada".equals(c.str("estado"))) {
                porAtender++;
            }
        }
        if (listos > 0) {
            l.add(listos + (listos == 1 ? " servicio listo para entrega" : " servicios listos para entrega"));
        }
        if (porAtender > 0) {
            l.add(porAtender + (porAtender == 1 ? " servicio por atender hoy" : " servicios por atender hoy"));
        }
        return l;
    }
}
