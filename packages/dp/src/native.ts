import { NativeModules, Platform } from 'react-native';

const LINKING_ERROR =
  `The package '@rn-csj/dp' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo managed workflow\n';

const Dp = NativeModules.Dp  ? NativeModules.Dp  : new Proxy(
  {},
  {
    get() {
      throw new Error(LINKING_ERROR);
    },
  }
);

export default Dp;
