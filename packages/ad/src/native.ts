import { NativeModules, Platform } from 'react-native';

const LINKING_ERROR =
  `The package '@rn-csj/ad' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo managed workflow\n';

const Ad = NativeModules.Ad  ? NativeModules.Ad  : new Proxy(
  {},
  {
    get() {
      throw new Error(LINKING_ERROR);
    },
  }
);

export default Ad;
