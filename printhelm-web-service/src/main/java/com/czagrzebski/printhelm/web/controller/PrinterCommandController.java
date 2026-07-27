package com.czagrzebski.printhelm.web.controller;

import com.czagrzebski.printhelm.web.service.PrinterCommandService;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/printer/{id}/command")
public class PrinterCommandController {

    private final PrinterCommandService commandService;

    public PrinterCommandController(PrinterCommandService commandService) {
        this.commandService = commandService;
    }

    record SpeedRequest(int speed) {}
    record JogRequest(String axis, double distance) {}
    record LightRequest(String node, String mode) {}
    record PrintFileRequest(String filename, int[] amsMapping, boolean flowCali, boolean vibrationCali, boolean layerInspect) {}
    record TempRequest(int temp) {}

    @PostMapping("/stop")
    public ResponseEntity<Void> stop(@PathVariable long id) throws MqttException {
        commandService.stopPrint(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/pause")
    public ResponseEntity<Void> pause(@PathVariable long id) throws MqttException {
        commandService.pausePrint(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/resume")
    public ResponseEntity<Void> resume(@PathVariable long id) throws MqttException {
        commandService.resumePrint(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/speed")
    public ResponseEntity<Void> speed(@PathVariable long id, @RequestBody SpeedRequest req) throws MqttException {
        commandService.setSpeed(id, req.speed());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/jog")
    public ResponseEntity<Void> jog(@PathVariable long id, @RequestBody JogRequest req) throws MqttException {
        commandService.jog(id, req.axis(), req.distance());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/home")
    public ResponseEntity<Void> home(@PathVariable long id) throws MqttException {
        commandService.home(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/nozzle-temp")
    public ResponseEntity<Void> nozzleTemp(@PathVariable long id, @RequestBody TempRequest req) throws MqttException {
        commandService.setNozzleTemp(id, req.temp());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bed-temp")
    public ResponseEntity<Void> bedTemp(@PathVariable long id, @RequestBody TempRequest req) throws MqttException {
        commandService.setBedTemp(id, req.temp());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/light")
    public ResponseEntity<Void> light(@PathVariable long id, @RequestBody LightRequest req) throws MqttException {
        commandService.setLight(id, req.node(), req.mode());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/print")
    public ResponseEntity<Void> printFile(@PathVariable long id, @RequestBody PrintFileRequest req) throws MqttException {
        commandService.printFile(id, req.filename(), req.amsMapping(), req.flowCali(), req.vibrationCali(), req.layerInspect());
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleNotConnected(IllegalStateException ex) {
        return ResponseEntity.status(409).body(ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
