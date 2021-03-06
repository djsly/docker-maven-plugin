package com.alexecollins.docker.component;

import com.alexecollins.docker.model.Conf;
import com.alexecollins.docker.model.Id;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.kpelykh.docker.client.DockerClient;
import com.kpelykh.docker.client.DockerException;
import com.kpelykh.docker.client.model.Container;
import com.kpelykh.docker.client.model.Image;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static java.util.Arrays.asList;

@SuppressWarnings("CanBeFinal")
public class Repo {

    private static ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());
    private final DockerClient docker;
    private final String prefix;
    private final File src;
    private final Map<Id, Conf> confs = new HashMap<Id, Conf>();

    @SuppressWarnings("ConstantConditions")
    public Repo(DockerClient docker, String prefix, File src) throws IOException {
        this.docker = docker;
        this.prefix = prefix;
        this.src = src;

        if (src.isDirectory()) {
            for (File file : src.listFiles()) {
                final File confFile = new File(file, "conf.yml");
                confs.put(new Id(file.getName()), confFile.length() > 0 ? MAPPER.readValue(confFile, Conf.class) : new Conf());
            }
        }
    }

    public String imageName(Id id) {
        return prefix + "_" + id;
    }

    public String containerName(Id id) {
        return "/" + prefix + "_" + id;
    }

    public List<Container> findContainers(Id id, boolean allContainers) {
        final List<Container> strings = new ArrayList<Container>();
        for (Container container : docker.listContainers(allContainers)) {
            if (container.getImage().equals(imageName(id)) || asList(container.getNames()).contains(containerName(id))) {
                strings.add(container);
            }
        }
        return strings;
    }

    public Container findContainer(Id id) {
        final List<Container> containerIds = findContainers(id, true);
        return containerIds.isEmpty() ? null : containerIds.get(0);
    }


    public Image findImage(Id id) throws DockerException {
        final List<Image> images = docker.getImages(imageName(id), true);
        return images.isEmpty() ? null : images.get(0);
    }

    public File src() {
        return src;
    }


    public File src(Id id) {
        return new File(src(), id.toString());
    }

    public List<Id> ids(boolean reverse) {

        final List<Id> in = new LinkedList<Id>(confs.keySet());

        final Map<Id, List<Id>> links = new HashMap<Id, List<Id>>();
        for (Id id : in) {
            links.put(id, confs.get(id).links);
        }

        final List<Id> out = sort(links);

        if (reverse) {
            Collections.reverse(out);
        }

        return out;
    }

    List<Id> sort(final Map<Id, List<Id>> links) {
        final List<Id> in = new LinkedList<Id>(links.keySet());
        final List<Id> out = new LinkedList<Id>();

        while (!in.isEmpty()) {
            boolean hit = false;
            for (Iterator<Id> iterator = in.iterator(); iterator.hasNext(); ) {
                final Id id = iterator.next();
                if (out.containsAll(links.get(id))) {
                    out.add(id);
                    iterator.remove();
                    hit = true;
                }
            }
            if (!hit) {
                throw new IllegalStateException("dependency error (e.g. circular dependency) amongst " + in);
            }
        }

        return out;
    }

    public Conf conf(Id id) {
        return confs.get(id);
    }
}
