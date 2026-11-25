package algo.vetplusapp;

import algo.vetplusapp.db.Row;
import algo.vetplusapp.repo.CitaRepo;
import algo.vetplusapp.util.Config;
import algo.vetplusapp.util.Sesion;
import java.util.ArrayList;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.ToggleButton;

public class MedicoShellController extends ShellBase {

    @FXML
    private ToggleButton btnMenuAgenda;
    @FXML
    private ToggleButton btnMenuConsulta;
    @FXML
    private ToggleButton btnMenuExpedientes;

    @FXML
    private void initialize() {
        registrar("agenda-medico", "Mi agenda", btnMenuAgenda);
        registrar("consulta-medica", "Consulta médica", btnMenuConsulta);
        registrar("expedientes-medico", "Expedientes", btnMenuExpedientes);
        iniciarMarco("agenda-medico");
    }

    @Override
    protected List<String> avisos() {
        List<String> l = new ArrayList<>();
        Row actual = CitaRepo.enAtencion(Sesion.usuarioId);
        if (actual != null) {
            l.add("Atención en curso: " + actual.str("mascota"));
        }
        int porAtender = 0;
        for (Row c : CitaRepo.citasDeProfesional(Sesion.usuarioId, Config.hoy())) {
            if ("Confirmada".equals(c.str("estado"))) {
                porAtender++;
            }
        }
        if (porAtender > 0) {
            l.add(porAtender + (porAtender == 1 ? " cita por atender hoy" : " citas por atender hoy"));
        }
        return l;
    }
}
