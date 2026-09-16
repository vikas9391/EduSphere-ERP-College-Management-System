import React from 'react';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { DashboardScreen } from './screens/DashboardScreen';
import { ClassesScreen } from './screens/ClassesScreen';
import { AttendanceScreen } from './screens/AttendanceScreen';
import { AssignmentsScreen } from './screens/AssignmentsScreen';
import { TimetableScreen } from './screens/TimetableScreen';
import { ProfileScreen } from './screens/ProfileScreen';

export type RootStackParamList = {
  Dashboard: undefined;
  Classes: undefined;
  Attendance: undefined;
  Assignments: undefined;
  Timetable: undefined;
  Profile: undefined;
};

const Stack = createNativeStackNavigator<RootStackParamList>();

export function AppNavigation() {
  return (
    <Stack.Navigator screenOptions={{ headerShown: false }}>
      <Stack.Screen name="Dashboard" component={DashboardScreen} />
      <Stack.Screen name="Classes" component={ClassesScreen} />
      <Stack.Screen name="Attendance" component={AttendanceScreen} />
      <Stack.Screen name="Assignments" component={AssignmentsScreen} />
      <Stack.Screen name="Timetable" component={TimetableScreen} />
      <Stack.Screen name="Profile" component={ProfileScreen} />
    </Stack.Navigator>
  );
}
