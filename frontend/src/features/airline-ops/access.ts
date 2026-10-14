import { createContext, useContext } from 'react'
import type { Airline } from './api'

// The airline-scoped permissions the flight pages act on, as seeded by user-service. The server enforces every one;
// the UI only uses them to decide what to show, so nobody is offered an action they would be refused.
export const Permission = {
  FlightRead: 'FLIGHT_READ',
  FlightManage: 'FLIGHT_MANAGE',
  ScheduleRead: 'FLIGHT_SCHEDULE_READ',
  ScheduleManage: 'FLIGHT_SCHEDULE_MANAGE',
  DepartureRead: 'FLIGHT_INSTANCE_READ',
  DepartureManage: 'FLIGHT_INSTANCE_MANAGE',
  AircraftAssign: 'FLIGHT_INSTANCE_AIRCRAFT_ASSIGN',
  AircraftRead: 'AIRCRAFT_READ',
} as const

export type Permission = (typeof Permission)[keyof typeof Permission]

export interface AirlineAccess {
  airline: Airline
  can: (permission: Permission) => boolean
}

export const AirlineAccessContext = createContext<AirlineAccess | null>(null)

export function useAirlineAccess() {
  const access = useContext(AirlineAccessContext)
  if (!access) throw new Error('useAirlineAccess must be used inside an airline workspace')
  return access
}
