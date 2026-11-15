package com.pfc.thindesk.service;

import com.pfc.thindesk.entity.Cliente;
import com.pfc.thindesk.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@Service
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private MongoTemplate mongoTemplate;

    public Cliente novoCliente(Cliente cliente) {
        cliente.setId(null);
        return clienteRepository.save(cliente);
    }

    public List<Cliente> listarClientes() {
        return clienteRepository.findAll();
    }

    public Cliente buscar(String id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado."));
    }

    public Cliente atualizar(String id, Cliente dados) {
        Cliente cliente = mongoTemplate.findAndModify(Query.query(Criteria.where("id").is(id)),
                new Update().set("nome", dados.getNome()).set("telefone", dados.getTelefone()).set("setor", dados.getSetor()),
                FindAndModifyOptions.options().returnNew(true), Cliente.class);
        if (cliente == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado.");
        }
        return cliente;
    }

    public void excluir(String id) {
        clienteRepository.delete(buscar(id));
    }
}
