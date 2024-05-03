package com.traffic.trafficapi.service.impl;

import com.traffic.trafficapi.domain.dto.CreateTrafficLight;
import com.traffic.trafficapi.domain.dto.CreateTrafficLightDependency;
import com.traffic.trafficapi.domain.model.Status;
import com.traffic.trafficapi.domain.model.TrafficDependencies;
import com.traffic.trafficapi.domain.model.TrafficLight;
import com.traffic.trafficapi.reposteries.TrafficDependenciesRepository;
import com.traffic.trafficapi.reposteries.TrafficLightRepository;
import com.traffic.trafficapi.service.TrafficLightService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@AllArgsConstructor
@Slf4j
public class DefaultTrafficLightService implements TrafficLightService {
    private final TrafficLightRepository trafficLightRepository;
    private final TrafficDependenciesRepository trafficDependenciesRepository;

    @Override
    public void saveTrafficLight(CreateTrafficLight trafficLight) {

        var trafficLightModel = new TrafficLight();
        trafficLightModel.setLocationName(trafficLight.getLocationName());
        trafficLightModel.setLongitude(trafficLight.getLongitude());
        trafficLightModel.setLatitude(trafficLight.getLatitude());
        trafficLightModel.setCreatedDate(LocalDateTime.now());
        trafficLightModel.setUpdatedAt(LocalDateTime.now());
        trafficLightModel.setStatus(Status.valueOf(trafficLight.getStatus().toUpperCase()));

        trafficLightRepository.save(trafficLightModel);
    }

    @Override
    public List<TrafficLight> getTrafficLights() {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(trafficLightRepository.findAll().iterator(), Spliterator.ORDERED), false)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<TrafficLight> getTrafficLight(Long id) {
        return trafficLightRepository.findById(id);
    }

    @Override
    public void deleteTrafficLight(Long id) {
        trafficLightRepository.deleteById(id);
    }

    @Override
    public List<TrafficLight> getTrafficLightsByLocationName(String locationName) {
        return trafficLightRepository.findTrafficLightByLocationName(locationName);
    }

    @Override
    public void createDependencies(CreateTrafficLightDependency dependencies) {
        List<Pair<Long, Long>> trafficLightsDependecies = dependencies.getTrafficLightsDependecies();

        for (Pair<Long, Long> trafficLightsDependecy : trafficLightsDependecies) {
            Long mainTrafficLightId = trafficLightsDependecy.getFirst();
            Long neighbourTrafficLightId = trafficLightsDependecy.getSecond();
            TrafficDependencies trafficDependencies = trafficDependenciesRepository.findByMainTrafficIdAndNeighbourTrafficId(mainTrafficLightId, neighbourTrafficLightId);

            if (trafficDependencies != null) {
                log.info("main and neighbour traffic light already found for " +
                                "main traffic id: {} and neighbour traffic id: {} ",
                        mainTrafficLightId,
                        neighbourTrafficLightId);
                continue;
            }
            TrafficLight mainTrafficLight = getTrafficLight(mainTrafficLightId) .orElseGet(() -> {
                log.info("Main traffic light not found for id: {}", mainTrafficLightId);
                return null;
            });;
            TrafficLight neighbourTrafficLight = getTrafficLight(neighbourTrafficLightId).orElseGet(() -> {
                log.info("Neighbour traffic light not found for id: {}", neighbourTrafficLightId);
                return null;
            });

            if (mainTrafficLight == null || neighbourTrafficLight == null) {
                continue;
            }

            if (mainTrafficLight.equals(neighbourTrafficLight)) {
                log.info("Main traffic light and neighbour traffic light cannot be the same");
                continue;
            }

            TrafficDependencies newTrafficDependency = new TrafficDependencies();
            newTrafficDependency.setMainTraffic(mainTrafficLight);
            newTrafficDependency.setNeighbourTraffic(neighbourTrafficLight);

            trafficDependenciesRepository.save(newTrafficDependency);
            log.info("Traffic dependency saved successfully");

        }
    }

    @Override
    public List<TrafficLight> getNeighbouringTrafficLights(Long id) {
        List<TrafficLight> neighboursTrafficLights = trafficDependenciesRepository.findNeighboursByMainTrafficLightId(id);
        if (neighboursTrafficLights.isEmpty()) {
            log.info("Neighbouring traffic light not found for id: {}", id);
        } else {
            log.info("Neighbouring traffic light found successfully for id: {}", id);
        }
        return neighboursTrafficLights;
    }
}
