/*
 * See LICENSE file in distribution for copyright and licensing information.
 */
package org.yaml.snakeyaml.constructor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.composer.Composer;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;

/**
 * because Java does not have generators 'deep' is dropped. Multi constructors
 * are not supported.
 * 
 * @see PyYAML 3.06 for more information
 */
public class BaseConstructor {
    protected Map<String, Construct> yamlConstructors = new HashMap<String, Construct>();

    private Composer composer;
    private Map<Node, Object> constructedObjects;
    private Map<Node, Object> recursiveObjects;

    public BaseConstructor() {
        constructedObjects = new HashMap<Node, Object>();
        recursiveObjects = new HashMap<Node, Object>();
    }

    public void setComposer(Composer composer) {
        this.composer = composer;
    }

    public boolean checkData() {
        // If there are more documents available?
        return composer.checkNode();
    }

    public Object getData() {
        // Construct and return the next document.
        if (composer.checkNode()) {
            Node node = composer.getNode();
            return constructDocument(node);
        }
        return null;
    }

    public Object getSingleData() {
        // Ensure that the stream contains a single document and construct it
        Node node = composer.getSingleNode();
        if (node != null) {
            return constructDocument(node);
        }
        return null;
    }

    private Object constructDocument(Node node) {
        Object data = constructObject(node);
        constructedObjects.clear();
        recursiveObjects.clear();
        return data;
    }

    protected Object constructObject(Node node) {
        if (constructedObjects.containsKey(node)) {
            return constructedObjects.get(node);
        }
        if (recursiveObjects.containsKey(node)) {
            throw new ConstructorException(null, null, "found unconstructable recursive node", node
                    .getStartMark());
        }
        recursiveObjects.put(node, null);
        Object data;
        Construct constructor = yamlConstructors.get(node.getTag());
        if (constructor == null) {
            if (yamlConstructors.containsKey(null)) {
                constructor = yamlConstructors.get(null);
                data = constructor.construct(node);
            } else if (node instanceof ScalarNode) {
                data = constructScalar((ScalarNode) node);
            } else if (node instanceof SequenceNode) {
                data = constructSequence((SequenceNode) node);
            } else if (node instanceof MappingNode) {
                data = constructMapping((MappingNode) node);
            } else {
                throw new YAMLException("Unknown node: " + node);
            }
        } else {
            data = constructor.construct(node);
        }
        constructedObjects.put(node, data);
        recursiveObjects.remove(node);
        return data;
    }

    protected Object constructScalar(ScalarNode node) {
        return node.getValue();
    }

    protected List<Object> constructSequence(SequenceNode node) {
        List<Node> nodeValue = (List<Node>) node.getValue();
        List<Object> result = createDefaultList(nodeValue.size());
        for (Iterator<Node> iter = nodeValue.iterator(); iter.hasNext();) {
            Node child = iter.next();
            result.add(constructObject(child));
        }
        return result;
    }

    protected List<Object> createDefaultList(int initSize) {
        return new LinkedList<Object>();
    }

    protected Map<Object, Object> constructMapping(MappingNode node) {
        // TODO should it be possible to customise Map implementation ?
        // respect order from YAML document
        Map<Object, Object> mapping = new LinkedHashMap<Object, Object>();
        List<Node[]> nodeValue = (List<Node[]>) node.getValue();
        for (Iterator<Node[]> iter = nodeValue.iterator(); iter.hasNext();) {
            Node[] tuple = iter.next();
            Node keyNode = tuple[0];
            Node valueNode = tuple[1];
            Object key = constructObject(keyNode);
            if (key != null) {
                int hash = key.hashCode();
                if (hash == 0) {
                    throw new ConstructorException("while constructing a mapping", node
                            .getStartMark(), "found unacceptable key " + key, tuple[0]
                            .getStartMark());
                }
            }
            Object value = constructObject(valueNode);
            mapping.put(key, value);
        }
        return mapping;
    }

    protected List<Object[]> constructPairs(MappingNode node) {
        List<Object[]> pairs = new LinkedList<Object[]>();
        List<Node[]> nodeValue = (List<Node[]>) node.getValue();
        for (Iterator<Node[]> iter = nodeValue.iterator(); iter.hasNext();) {
            Node[] tuple = iter.next();
            Object key = constructObject(tuple[0]);
            Object value = constructObject(tuple[1]);
            pairs.add(new Object[] { key, value });
        }
        return pairs;
    }
}