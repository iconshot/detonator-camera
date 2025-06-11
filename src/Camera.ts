import $ from "untrue";

import Detonator, { BaseView, ViewProps } from "detonator";

export interface CameraMedia {
  source: string;
  width: number;
  height: number;
}

export interface CameraProps extends ViewProps {
  back?: boolean | null;
}

export class Camera extends BaseView<CameraProps> {
  public async takePhoto(): Promise<CameraMedia> {
    return await Detonator.request(
      { name: "com.iconshot.detonator.camera::takePhoto" },
      this
    );
  }

  public async startRecording(): Promise<CameraMedia> {
    return await Detonator.request(
      { name: "com.iconshot.detonator.camera::startRecording" },
      this
    );
  }

  public async stopRecording(): Promise<void> {
    await Detonator.request(
      { name: "com.iconshot.detonator.camera::stopRecording" },
      this
    );
  }

  public render(): any {
    const { children, ...attributes } = this.props;

    return $("com.iconshot.detonator.camera", attributes, children);
  }

  public static async requestPermissions(): Promise<boolean> {
    return await Detonator.request({
      name: "com.iconshot.detonator.camera::requestPermissions",
    });
  }
}
