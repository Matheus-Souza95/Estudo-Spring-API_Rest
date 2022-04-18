package com.api.parkingcontrol.DTO;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Getter
@Setter
public class UserForm implements Serializable {

    @NotBlank(message = "Este campo nao pode ser vazio")
    private String name;
    @NotBlank(message = "Este campo nao pode ser vazio")
    private String cpf;

}
