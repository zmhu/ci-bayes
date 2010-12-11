package com.enigmastation.neuralnet.service.impl;

import com.enigmastation.neuralnet.dao.NeuronDAO;
import com.enigmastation.neuralnet.dao.SynapseDAO;
import com.enigmastation.neuralnet.model.Neuron;
import com.enigmastation.neuralnet.model.Synapse;
import com.enigmastation.neuralnet.model.Visibility;
import com.enigmastation.neuralnet.service.NeuralNetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Service
public class NeuralNetServiceImpl implements NeuralNetService {
    @Autowired
    NeuronDAO neuronDAO;
    @Autowired
    SynapseDAO synapseDAO;

    @Override
    public Neuron getNeuronById(String id) {
        return neuronDAO.readById(id);
    }

    @Override
    public Neuron getNeuron(String payload, Visibility visibility) {
        return getNeuron(payload, visibility, false);
    }

    @Override
    @Transactional
    public void setStrength(Neuron from, Neuron to, double strength) {
        Synapse template = new Synapse();
        template.setFrom(from.getId());
        template.setTo(to.getId());
        synapseDAO.take(template);
        template.setStrength(strength);
        synapseDAO.write(template);
    }

    @Override
    public Set<Synapse> getSynapsesFrom(Neuron neuron) {
        if (neuron == null) {
            return new HashSet<Synapse>(0);
        }
        //System.out.println("getSynapsesFrom("+neuron+")");
        Set<Synapse> synapses = new HashSet<Synapse>();
        Synapse template = new Synapse();
        template.setFrom(neuron.getId());
        Synapse[] synapseArray = synapseDAO.readMultiple(template);

        synapses.addAll(Arrays.asList(synapseArray));

        return synapses;
    }

    @Override
    public double getStrength(Neuron from, Neuron to, Visibility visibility) {
        Synapse template = new Synapse();
        template.setFrom(from.getId());
        template.setTo(to.getId());
        Synapse r = synapseDAO.read(template);
        if (r == null) {
            return visibility.getStrength();
        }
        return r.getStrength();
    }

    @Override
    public void reset() {
        synapseDAO.takeMultiple(new Synapse());
        neuronDAO.takeMultiple(new Neuron());
    }

    @Override
    @Transactional
    public Neuron getNeuron(String payload, Visibility visibility, boolean createIfNecessary) {
        Neuron template = new Neuron();
        template.setPayload(payload);
        template.setVisibility(visibility);
        Neuron neuron = neuronDAO.read(template);
        if (neuron == null && createIfNecessary) {
            neuron = neuronDAO.write(template);
        }
        return neuron;
    }
}