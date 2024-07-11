package br.com.rockectseat.journey.trip;

import br.com.rockectseat.journey.activity.ActivityData;
import br.com.rockectseat.journey.activity.ActivityRequestPayload;
import br.com.rockectseat.journey.activity.ActivityResponse;
import br.com.rockectseat.journey.activity.ActivityService;
import br.com.rockectseat.journey.link.LinkData;
import br.com.rockectseat.journey.link.LinkRequestPayload;
import br.com.rockectseat.journey.link.LinkResponse;
import br.com.rockectseat.journey.link.LinkService;
import br.com.rockectseat.journey.participant.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/trips")
public class TripController {

    @Autowired
    private ParticipantService participantService;

    @Autowired
    private TripRepository repository;

    @Autowired
    private ActivityService activityService;

    @Autowired
    private LinkService linkService;

    @PostMapping
    public ResponseEntity<TripCreateResponse> createTrip (@RequestBody TripRequestPayload payload) {
        Trip newTrip = new Trip(payload);
        this.repository.save(newTrip);
        this.participantService.registerParticipantsToEvent(payload.emails_to_invite(), newTrip);
        return ResponseEntity.ok(new TripCreateResponse(newTrip.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Trip> getTripDetails (@PathVariable UUID id) {
        Optional<Trip> trip = this.repository.findById(id);
        return trip.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Trip> updateTrip (@PathVariable UUID id, @RequestBody TripRequestPayload payload) {
        Optional<Trip> trip = this.repository.findById(id);
        if (trip.isPresent()) {
            Trip rawTrip = trip.get();
            rawTrip.setEndsAt(LocalDateTime.parse(payload.ends_at(), DateTimeFormatter.ISO_DATE_TIME));
            rawTrip.setStartsAt(LocalDateTime.parse(payload.start_at(), DateTimeFormatter.ISO_DATE_TIME));
            rawTrip.setDestination(payload.destination());

            this.repository.save(rawTrip);
            return ResponseEntity.ok(rawTrip);
        }

        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/confirm")
    public ResponseEntity<Trip> confirmTrip (@PathVariable UUID id) {
        Optional<Trip> trip = this.repository.findById(id);

        if (trip.isPresent()) {
            Trip rawTrip = trip.get();
            rawTrip.setConfirmed(true);
            this.repository.save(rawTrip);
            this.participantService.triggerConfirmationEmailToParticipants(id);
            return ResponseEntity.ok(rawTrip);
        }

        return ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/invite")
    public ResponseEntity<ParticipantCreateResponse> inviteParticipant (@PathVariable UUID id, @RequestBody ParticipantRequestPayload payload)  {
        Optional<Trip> participant = this.repository.findById(id);
        if (participant.isPresent()) {
            Trip rawTrip = participant.get();
            ParticipantCreateResponse response = this.participantService.registerParticipantToEvent(payload.email(), rawTrip);

            if (rawTrip.isConfirmed()) this.participantService.triggerConfirmationEmailToParticipant(payload.email());
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/participants")
    public ResponseEntity<List<ParticipantData>> getAllParticipant (@PathVariable UUID id) {
        List<ParticipantData> participants = this.participantService.getAllParticipantFromEvent(id);

        return ResponseEntity.ok(participants);
    }

    @GetMapping("/{id}/activities")
    public ResponseEntity<List<ActivityData>> getAllActivities (@PathVariable UUID id) {
        List<ActivityData> activityDataList = this.activityService.getAllActivitiesFromId(id);

        return ResponseEntity.ok(activityDataList);
    }

    @PostMapping("/{id}/activities")
    public ResponseEntity<ActivityResponse> registerActivity (@PathVariable UUID id, @RequestBody ActivityRequestPayload payload)  {
        Optional<Trip> participant = this.repository.findById(id);
        if (participant.isPresent()) {
            Trip rawTrip = participant.get();
            ActivityResponse response = this.activityService.saveActivity(payload, rawTrip);
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/links")
    public ResponseEntity<LinkResponse> registerActivity (@PathVariable UUID id, @RequestBody LinkRequestPayload payload)  {
        Optional<Trip> participant = this.repository.findById(id);
        if (participant.isPresent()) {
            Trip rawTrip = participant.get();
            LinkResponse response = this.linkService.registerLink(payload, rawTrip);
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/links")
    public ResponseEntity<List<LinkData>> getAllLinks (@PathVariable UUID id) {
        List<LinkData> linkDataList = this.linkService.getAllLinkFromId(id);

        return ResponseEntity.ok(linkDataList);
    }
}
