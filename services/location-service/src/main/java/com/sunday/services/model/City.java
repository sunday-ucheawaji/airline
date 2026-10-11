package com.sunday.services.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.ZoneId;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.UuidGenerator;
import java.util.UUID;

@Entity
@Table(name = "cities", indexes = {
        @Index(name = "idx_city_code", columnList = "cityCode"),
        @Index(name = "idx_city_name", columnList = "name"),
        @Index(name = "idx_country_code", columnList = "countryCode")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class City {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String cityCode;

    @Column(nullable = false)
    private String countryCode;

    @Column(nullable = false)
    private String countryName;

    private String regionCode;

    @Column(name = "time_zone_id", length = 50)
    private String timeZoneId;

    @Transient
    @com.fasterxml.jackson.annotation.JsonIgnore
    public ZoneId getTimeZone() {
        return timeZoneId != null ? ZoneId.of(timeZoneId) : null;
    }

    public void setTimeZone(ZoneId zoneId) {
        this.timeZoneId = zoneId != null ? zoneId.getId() : null;
    }

    @Transient
    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getCurrentUtcOffset() {
        if (timeZoneId != null) {
            ZoneId zone = ZoneId.of(timeZoneId);
            return zone.getRules().getOffset(Instant.now()).toString();
        }
        return null;
    }

    @Transient
    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getStandardUtcOffset() {
        if (timeZoneId != null) {
            ZoneId zone = ZoneId.of(timeZoneId);
            return zone.getRules().getStandardOffset(Instant.now()).toString();
        }
        return null;
    }

    @Transient
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean observesDaylightSaving() {
        if (timeZoneId != null) {
            ZoneId zone = ZoneId.of(timeZoneId);
            return !zone.getRules().getTransitionRules().isEmpty();
        }
        return false;
    }
}
