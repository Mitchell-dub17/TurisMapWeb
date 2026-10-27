package com.turismap.admin.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.google.cloud.Timestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Resena {
    @NotBlank(message = "El tipo de destino es obligatorio")
    private String tipoDestino; // lugar, comercio
    
    @NotBlank(message = "El destino ID es obligatorio")
    private String destinoId;
    
    @NotBlank(message = "El usuario UID es obligatorio")
    private String usuarioUid;
    
    private String comentario;
    
    @Min(value = 1, message = "La calificación mínima es 1")
    @Max(value = 5, message = "La calificación máxima es 5")
    private Integer calificacion;
    
    private Timestamp creadoEn;
    private Timestamp actualizadoEn;
}
