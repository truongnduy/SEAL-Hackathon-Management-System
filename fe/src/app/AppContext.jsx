import React, { createContext, useContext, useState, useEffect } from "react";
import dayjs from "dayjs";
import { MOCK_HACKATHONS } from "../features/hackathons/data/hackathon.mock";
import { MOCK_TRACKS } from "../features/tracks/data/track.mock";
import { MOCK_ROUNDS } from "../features/rounds/data/round.mock";
import { MOCK_CRITERIA } from "../features/criteria/data/criteria.mock";
import { MOCK_PEOPLE } from "../features/people/data/people.mock";
import { MOCK_ASSIGNMENTS } from "../features/people/data/assignments.mock";
import { MOCK_EVENTS } from "../features/events/data/event.mock";
import { MOCK_NOTIFICATIONS } from "../features/notifications/data/notification.mock";
import { notificationService } from "../features/notifications/services/notificationService";

const AppContext = createContext();

export const AppProvider = ({ children }) => {
  const [darkMode, setDarkMode] = useState(() => {
    const saved = localStorage.getItem("darkMode");
    return saved === "true";
  });

  const toggleDarkMode = () => {
    setDarkMode((prev) => !prev);
  };

  useEffect(() => {
    localStorage.setItem("darkMode", darkMode);
  }, [darkMode]);

  const [hackathons, setHackathons] = useState(() => {
    const saved = localStorage.getItem("hackathons");
    return saved ? JSON.parse(saved) : MOCK_HACKATHONS;
  });

  const [tracks, setTracks] = useState(() => {
    const saved = localStorage.getItem("tracks");
    return saved ? JSON.parse(saved) : MOCK_TRACKS;
  });

  const [rounds, setRounds] = useState(() => {
    const saved = localStorage.getItem("rounds");
    return saved ? JSON.parse(saved) : MOCK_ROUNDS;
  });

  const [criteria, setCriteria] = useState(() => {
    const saved = localStorage.getItem("criteria");
    return saved ? JSON.parse(saved) : MOCK_CRITERIA;
  });

  const [people, setPeople] = useState(() => {
    const saved = localStorage.getItem("people");
    return saved ? JSON.parse(saved) : MOCK_PEOPLE;
  });

  const [assignments, setAssignments] = useState(() => {
    const saved = localStorage.getItem("assignments");
    return saved ? JSON.parse(saved) : MOCK_ASSIGNMENTS;
  });

  const [events, setEvents] = useState(() => {
    const saved = localStorage.getItem("events");
    return saved ? JSON.parse(saved) : MOCK_EVENTS;
  });

  const [notifications, setNotifications] = useState(() => {
    const saved = localStorage.getItem("notifications");
    return saved ? JSON.parse(saved) : MOCK_NOTIFICATIONS;
  });

  const [teams, setTeams] = useState(() => {
    const saved = localStorage.getItem("teams");
    return saved ? JSON.parse(saved) : [];
  });

  useEffect(() => {
    localStorage.setItem("hackathons", JSON.stringify(hackathons));
  }, [hackathons]);

  useEffect(() => {
    localStorage.setItem("tracks", JSON.stringify(tracks));
  }, [tracks]);

  useEffect(() => {
    localStorage.setItem("rounds", JSON.stringify(rounds));
  }, [rounds]);

  useEffect(() => {
    localStorage.setItem("criteria", JSON.stringify(criteria));
  }, [criteria]);
  useEffect(() => {
    localStorage.setItem("people", JSON.stringify(people));
  }, [people]);
  useEffect(() => {
    localStorage.setItem("assignments", JSON.stringify(assignments));
  }, [assignments]);
  useEffect(() => {
    localStorage.setItem("events", JSON.stringify(events));
  }, [events]);
  useEffect(() => {
    localStorage.setItem("notifications", JSON.stringify(notifications));
  }, [notifications]);

  useEffect(() => {
    const token = localStorage.getItem("accessToken");
    if (!token) return;

    notificationService
      .list()
      .then((items) => {
        if (Array.isArray(items) && items.length > 0) {
          setNotifications(items);
        }
      })
      .catch(() => {
        setNotifications((prev) => (prev?.length ? prev : MOCK_NOTIFICATIONS));
      });
  }, []);
  useEffect(() => {
    localStorage.setItem("teams", JSON.stringify(teams));
  }, [teams]);

  // Auto-update status based on real time
  useEffect(() => {
    const checkStatus = () => {
      const now = dayjs();
      let changed = false;

      // Update Hackathons
      const updatedHackathons = hackathons.map((h) => {
        if (
          h.event_end &&
          dayjs(h.event_end).isBefore(now) &&
          h.status !== "FINISHED"
        ) {
          changed = true;
          return { ...h, status: "FINISHED" };
        }
        return h;
      });

      // Update Tracks (Close if hackathon completed)
      const updatedTracks = tracks.map((t) => {
        const hackathon = updatedHackathons.find(
          (h) => h.id === t.hackathon_id,
        );
        if (hackathon?.status === "FINISHED" && t.status !== "CLOSED") {
          changed = true;
          return { ...t, status: "CLOSED" };
        }
        return t;
      });

      // Update Rounds (Deactivate if deadline passed)
      const updatedRounds = rounds.map((r) => {
        if (
          r.submission_deadline &&
          dayjs(r.submission_deadline).isBefore(now) &&
          r.is_active
        ) {
          changed = true;
          return { ...r, is_active: false };
        }
        return r;
      });

      if (changed) {
        setHackathons(updatedHackathons);
        setTracks(updatedTracks);
        setRounds(updatedRounds);
      }
    };

    // Check immediately and then every minute
    checkStatus();
    const interval = setInterval(checkStatus, 60000);
    return () => clearInterval(interval);
  }, [hackathons, tracks, rounds]);

  // Hackathon actions
  const addHackathon = (hackathon) => {
    const newHackathon = {
      ...hackathon,
      id:
        hackathons.length > 0
          ? Math.max(...hackathons.map((h) => h.id)) + 1
          : 1,
      status: "DRAFT",
    };
    setHackathons([...hackathons, newHackathon]);
    return newHackathon;
  };

  const updateHackathon = (id, updates) => {
    setHackathons(
      hackathons.map((h) => (h.id === id ? { ...h, ...updates } : h)),
    );
  };

  const deleteHackathon = (id) => {
    setHackathons(hackathons.filter((h) => h.id !== id));
    setTracks(tracks.filter((t) => t.hackathon_id !== id));
    // Rounds are linked to tracks, so we need to filter them too
    const trackIds = tracks
      .filter((t) => t.hackathon_id === id)
      .map((t) => t.id);
    const roundIds = rounds
      .filter((r) => trackIds.includes(r.track_id))
      .map((r) => r.id);
    setRounds(rounds.filter((r) => !trackIds.includes(r.track_id)));
    setCriteria(criteria.filter((c) => !roundIds.includes(c.round_id)));
  };

  // Track actions
  const addTrack = (track) => {
    const newTrack = {
      ...track,
      id: tracks.length > 0 ? Math.max(...tracks.map((t) => t.id)) + 1 : 1,
      status: "OPEN",
    };
    setTracks([...tracks, newTrack]);
    return newTrack;
  };

  const updateTrack = (id, updates) => {
    setTracks(tracks.map((t) => (t.id === id ? { ...t, ...updates } : t)));
  };

  const deleteTrack = (id) => {
    setTracks(tracks.filter((t) => t.id !== id));
    const roundIds = rounds.filter((r) => r.track_id === id).map((r) => r.id);
    setRounds(rounds.filter((r) => r.track_id !== id));
    setCriteria(criteria.filter((c) => !roundIds.includes(c.round_id)));
  };

  // Round actions
  const addRound = (round) => {
    const newRound = {
      ...round,
      id: rounds.length > 0 ? Math.max(...rounds.map((r) => r.id)) + 1 : 1,
      is_active: false,
      tiebreak_rule: round.tiebreak_rule || "PENALTY_SCORE",
    };
    setRounds([...rounds, newRound]);
    return newRound;
  };

  const updateRound = (id, updates) => {
    setRounds(rounds.map((r) => (r.id === id ? { ...r, ...updates } : r)));
  };

  const deleteRound = (id) => {
    setRounds(rounds.filter((r) => r.id !== id));
    setCriteria(criteria.filter((c) => c.round_id !== id));
  };

  // Criteria actions
  const addCriteria = (criterion) => {
    const newCriterion = {
      ...criterion,
      id: criteria.length > 0 ? Math.max(...criteria.map((c) => c.id)) + 1 : 1,
    };
    setCriteria([...criteria, newCriterion]);
    return newCriterion;
  };

  const updateCriteria = (id, updates) => {
    setCriteria(criteria.map((c) => (c.id === id ? { ...c, ...updates } : c)));
  };

  const deleteCriteria = (id) => {
    setCriteria(criteria.filter((c) => c.id !== id));
  };

  // People actions
  const addPerson = (person) => {
    const newPerson = { ...person, id: Date.now(), status: "PENDING" };
    setPeople([...people, newPerson]);
    return newPerson;
  };

  const assignRole = (assignment) => {
    const newAssignment = { ...assignment, id: Date.now() };
    setAssignments([...assignments, newAssignment]);
  };

  const removeAssignment = (id) => {
    setAssignments(assignments.filter((a) => a.id !== id));
  };

  // --- Event actions ---
  const addEvent = (event) => {
    const newEvent = { ...event, id: Date.now() };
    setEvents([...events, newEvent]);
    return newEvent;
  };

  // Notification actions
  const addNotification = (notif) => {
    const newNotif = {
      ...notif,
      id: Date.now(),
      time: "Vừa xong",
      is_read: false,
    };
    setNotifications((prev) => [newNotif, ...prev]); // Push lên đầu mảng
  };

  const markAsRead = async (id) => {
    const ids =
      id === "ALL"
        ? notifications.filter((n) => !n.is_read).map((n) => n.id)
        : [id];

    if (ids.length > 0) {
      try {
        await notificationService.markRead(ids);
      } catch (error) {
        console.warn("Không thể đánh dấu đã đọc trên server:", error);
      }
    }

    if (id === "ALL") {
      setNotifications((prev) => prev.map((n) => ({ ...n, is_read: true })));
    } else {
      setNotifications((prev) =>
        prev.map((n) => (n.id === id ? { ...n, is_read: true } : n)),
      );
    }
  };

  const addTeam = (team) => {
    const newTeam = {
      ...team,
      id: teams.length > 0 ? Math.max(...teams.map((t) => t.id)) + 1 : 1,
      status: "PENDING",
      isLocked: false,
      createdAt: new Date().toISOString(),
    };
    setTeams([...teams, newTeam]);
    return newTeam;
  };

  const updateTeam = (id, updates) => {
    setTeams(teams.map((t) => (t.id === id ? { ...t, ...updates } : t)));
  };

  const deleteTeam = (id) => {
    setTeams(teams.filter((t) => t.id !== id));
  };

  return (
    <AppContext.Provider
      value={{
        darkMode,
        toggleDarkMode,
        hackathons,
        addHackathon,
        updateHackathon,
        deleteHackathon,
        tracks,
        addTrack,
        updateTrack,
        deleteTrack,
        rounds,
        addRound,
        updateRound,
        deleteRound,
        criteria,
        addCriteria,
        updateCriteria,
        deleteCriteria,
        people,
        addPerson,
        assignments,
        assignRole,
        removeAssignment,
        events,
        addEvent,
        notifications,
        addNotification,
        markAsRead,
        teams,
        addTeam,
        updateTeam,
        deleteTeam
      }}
    >
      {children}
    </AppContext.Provider>
  );
};

export const useAppContext = () => useContext(AppContext);
