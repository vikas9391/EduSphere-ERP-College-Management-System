import React from 'react';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { DashboardScreen } from './screens/DashboardScreen';
import { TeacherDashboardScreen } from './screens/TeacherDashboardScreen';
import { AdminDashboardScreen } from './screens/AdminDashboardScreen';
import { ClassesScreen } from './screens/ClassesScreen';
import { AttendanceScreen } from './screens/AttendanceScreen';
import { AssignmentsScreen } from './screens/AssignmentsScreen';
import { TimetableScreen } from './screens/TimetableScreen';
import { ProfileScreen } from './screens/ProfileScreen';

export type RootStackParamList = { Dashboard:undefined; TeacherDashboard:undefined; AdminDashboard:undefined; Classes:undefined; Attendance:undefined; Assignments:undefined; Timetable:undefined; Profile:undefined };
const Stack = createNativeStackNavigator<RootStackParamList>();

export function AppNavigation({ role }: { role?: string | null }) {
  const normalized = (role ?? '').toUpperCase().replace(/^ROLE_/, '');
  const home = normalized === 'TEACHER' ? 'TeacherDashboard' : ['ADMIN','SUPER_ADMIN'].includes(normalized) ? 'AdminDashboard' : 'Dashboard';
  return <Stack.Navigator initialRouteName={home as keyof RootStackParamList} screenOptions={{ headerShown:false }}>
    <Stack.Screen name="Dashboard" component={DashboardScreen}/>
    <Stack.Screen name="TeacherDashboard" component={TeacherDashboardScreen}/>
    <Stack.Screen name="AdminDashboard" component={AdminDashboardScreen}/>
    <Stack.Screen name="Classes" component={ClassesScreen}/>
    <Stack.Screen name="Attendance" component={AttendanceScreen}/>
    <Stack.Screen name="Assignments" component={AssignmentsScreen}/>
    <Stack.Screen name="Timetable" component={TimetableScreen}/>
    <Stack.Screen name="Profile" component={ProfileScreen}/>
  </Stack.Navigator>;
}
