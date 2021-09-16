package com.ynero.ss.execution.services.sl;

import com.ynero.ss.execution.domain.Node;
import com.ynero.ss.execution.domain.dto.NodeDTO;
import com.ynero.ss.execution.domain.dto.NodeGetDTO;
import com.ynero.ss.execution.persistence.node.NodeRepository;
import lombok.Setter;
import lombok.SneakyThrows;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class NodeServiceImpl implements NodeService {

    @Setter(onMethod_ = {@Autowired})
    private NodeRepository nodeRepository;

    @Setter(onMethod_ = {@Autowired})
    private ModelMapper modelMapper;

    public boolean update(NodeDTO dto, String nodeId) {
        var node = modelMapper.map(dto, Node.class);
        node.setNodeId(UUID.fromString(nodeId));
        var result = nodeRepository.update(node);
        return result;
    }

    public boolean delete(String nodeId) {
        var id = UUID.fromString(nodeId);
        var result = nodeRepository.delete(id);
        return result;
    }

    public String save(NodeDTO dto) {
        var node = modelMapper.map(dto, Node.class);
        if (node.getNodeId() == null) {
            node.setNodeId(UUID.randomUUID());
        }
        node = nodeRepository.save(node);
        return node.getNodeId().toString();
    }

    @SneakyThrows
    public NodeGetDTO findById(String nodeId) {
        var id = UUID.fromString(nodeId);
        var node = nodeRepository.findByNodeId(id);
        if (!node.isEmpty()) {
            var nodeDTO = modelMapper.map(node.get(), NodeGetDTO.class);
            return nodeDTO;
        }
        throw new Exception("No such node");
    }
}
