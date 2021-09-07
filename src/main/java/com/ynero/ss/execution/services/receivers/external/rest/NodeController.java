package com.ynero.ss.execution.services.receivers.external.rest;

import com.ynero.ss.execution.domain.dto.NodeDTO;
import com.ynero.ss.execution.domain.dto.NodeGetDTO;
import com.ynero.ss.execution.services.sl.NodeService;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("nodes")
public class NodeController {

    @Setter(onMethod_ = {@Autowired})
    private NodeService nodeService;

    @PostMapping
    private ResponseEntity<String> create(@RequestBody NodeDTO dto) {
        var nodeId = nodeService.save(dto);
        return new ResponseEntity<String>(nodeId, HttpStatus.OK);
    }

    @DeleteMapping
    private ResponseEntity delete(@RequestParam String nodeId) {
        var nodeWasDeleted = nodeService.delete(nodeId);
        return nodeWasDeleted ? ResponseEntity.ok().build() : ResponseEntity.badRequest().build();
    }

    @GetMapping
    private ResponseEntity<NodeGetDTO> find(@RequestParam String nodeId) {
        var node = nodeService.findById(nodeId);
        return node==null ? ResponseEntity.badRequest().build() :
                ResponseEntity.ok(node);
    }

    @PutMapping
    private ResponseEntity update(@RequestBody NodeDTO dto, @RequestParam String nodeId) {
        var result = nodeService.update(dto, nodeId);
        return result ? ResponseEntity.ok().build() : ResponseEntity.badRequest().build();
    }
}