import $, { Ref } from "untrue";

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
  private elementRef: Ref<Element> = new Ref();

  public async takePhoto(): Promise<CameraMedia> {
    const element = this.elementRef.value;

    if (element === null) {
      throw new Error("Camera element is not mounted.");
    }

    return await Detonator.request("com.iconshot.detonator.camera::takePhoto")
      .withEdge(element)
      .fetchAndDecode();
  }

  public async startRecording(): Promise<CameraMedia> {
    const element = this.elementRef.value;

    if (element === null) {
      throw new Error("Camera element is not mounted.");
    }

    return await Detonator.request(
      "com.iconshot.detonator.camera::startRecording"
    )
      .withEdge(element)
      .fetchAndDecode();
  }

  public async stopRecording(): Promise<void> {
    const element = this.elementRef.value;

    if (element === null) {
      throw new Error("Camera element is not mounted.");
    }

    await Detonator.request("com.iconshot.detonator.camera::stopRecording")
      .withEdge(element)
      .fetch();
  }

  public render(): any {
    const { children, ...attributes } = this.props;

    const tmpAttributes = { ...attributes, ref: this.elementRef };

    return $("com.iconshot.detonator.camera", tmpAttributes, children);
  }

  public static async requestPermissions(): Promise<boolean> {
    return await Detonator.request(
      "com.iconshot.detonator.camera::requestPermissions"
    ).fetchAndDecode();
  }
}
