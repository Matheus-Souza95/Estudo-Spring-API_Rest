package com.api.parkingcontrol.controllers;

import com.api.parkingcontrol.DTO.UserForm;
import com.api.parkingcontrol.models.CarModel;
import com.api.parkingcontrol.models.ParkingSpotModel;
import com.api.parkingcontrol.models.UserModel;
import com.api.parkingcontrol.services.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@EntityScan("com.api.parkingcontrol")
@RestController
@CrossOrigin(originPatterns = "*", maxAge = 3600)
@RequestMapping("/parking-control")
public class UserController {

    final UserService userService;

    @Autowired
    public UserController(UserService userService) {

        this.userService = userService;
    }

    @PostMapping(("user/registration"))
    public ResponseEntity<Object> saveUser(@RequestBody @Valid UserForm userSource) {

        if (userService.existsByCpf(userSource.getCpf())) {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("CPF ja esta em uso");
        }

        UserModel userTarget = new UserModel();
        BeanUtils.copyProperties(userSource, userTarget);

        userService.save(userTarget);

        return ResponseEntity.status(HttpStatus.CREATED).body(userTarget);
    }

    @GetMapping("user/all")
    public ResponseEntity<Page<UserModel>> getAllUser(@PageableDefault(page = 0, size = 2, direction = Sort.Direction.ASC) Pageable pageable) {
        Page<UserModel> userList = userService.findAll(pageable);
        if (userList.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } else {
            for (UserModel user : userList) {
                long id = user.getId();
                user.add(linkTo(methodOn(ParkingSpotController.class).getById(user.getId())).withSelfRel());
            }
        }
        return ResponseEntity.ok().body(userList);
    }

    @GetMapping("user/{id}")
    public ResponseEntity<Object> getById(@PathVariable(value = "id") long id) {
        Optional<UserModel> userOptional = userService.findById(id);
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Vaga nao encontrada");
        }

        userOptional.get().add(linkTo(methodOn(ParkingSpotController.class).getById(id)).withSelfRel());
        return ResponseEntity.ok().body(userOptional);
    }

    @PatchMapping("user/patch/{id}")
    @Transactional
    public ResponseEntity<Object> patchUser(@RequestBody Map<String, String> sourceFields, @PathVariable(value = "id") long id) {
        Optional<UserModel> userOptional = userService.findById(id);
        if (userService.existsByCpf(sourceFields.get("cpf"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("CPF ja cadastrado");
        }
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Usuario nao encontrado");
        }

        sourceFields.forEach((key, value) -> {
            Field field = ReflectionUtils.findField(UserModel.class, key);
            assert field != null;
            field.setAccessible(true);
            ReflectionUtils.setField(field, userOptional.get(), value);
        });

        userService.save(userOptional.get());

        userOptional.get().add(linkTo(methodOn(CarController.class).getById(id)).withSelfRel());
        return ResponseEntity.status(HttpStatus.OK).body(userOptional.get());

    }
}
