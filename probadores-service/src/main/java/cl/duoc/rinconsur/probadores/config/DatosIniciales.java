package cl.duoc.rinconsur.probadores.config;

import cl.duoc.rinconsur.probadores.model.EstadoProbador;
import cl.duoc.rinconsur.probadores.model.Probador;
import cl.duoc.rinconsur.probadores.model.TipoProbador;
import cl.duoc.rinconsur.probadores.repository.ProbadorRepository;
import cl.duoc.rinconsur.probadores.service.ProbadorService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.stream.IntStream;

// La primera vez crea 8 probadores libres: P-01 a P-07 estándar y P-08 accesible (datos ficticios).
@Component
@RequiredArgsConstructor
public class DatosIniciales implements CommandLineRunner {

    private final ProbadorRepository probadorRepository;

    @Override
    public void run(String... args) {
        if (probadorRepository.count() == 0) {
            probadorRepository.saveAll(IntStream.rangeClosed(1, 8)
                    .mapToObj(n -> Probador.builder()
                            .codigo(ProbadorService.codigo(n))
                            .tipo(n == 8 ? TipoProbador.ACCESIBLE : TipoProbador.ESTANDAR)
                            .estado(EstadoProbador.LIBRE)
                            .build())
                    .toList());
        }
    }
}
