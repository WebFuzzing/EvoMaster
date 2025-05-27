import matplotlib.pyplot as plt
import seaborn as sns
import time
import os

csv_file = "samples.csv"
green_values = []
red_values = []

plt.ion()  # Turn on interactive mode
fig, ax = plt.subplots(figsize=(10, 5))
plt.show()

# Wait until the file is available
while not os.path.exists(csv_file):
    print("Waiting for samples.csv...")
    time.sleep(0.1)
try:
    with open(csv_file, "r") as f:
        header = f.readline()  # Skip "x,result" header

        while True:
            line = f.readline()
            if not line:
                time.sleep(0.005)
                continue

            try:
                x_str, result_str = line.strip().split(",")
                x = int(x_str)
                result = int(result_str)
                print(f"Read: x={x}, result={result}")
            except:
                continue  # Skip malformed lines

            if result == 200:
                green_values.append(x)
            else:
                red_values.append(x)

            all_x = green_values + red_values
            if len(green_values) < 2 or len(all_x) < 2:
                continue

            # Compute x range
            x_min = min(all_x)
            x_max = max(all_x)
            margin = max((x_max - x_min) * 0.05, 10)
            x_left = x_min - margin
            x_right = x_max + margin

            # Clear and plot
            ax.clear()
            ax.set_title("Live KDE and dots on x-axis")
            ax.set_xlabel("x value")
            ax.set_ylabel("Density")
            ax.set_ylim(bottom=0)

            if len(all_x) >= 2:
                try:
                    sns.kdeplot(
                        all_x,
                        ax=ax,
                        color="blue",
                        fill=True,
                        bw_adjust=0.8,
                        label="KDE (all x)"
                    )
                except Exception as e:
                    print(f"KDE error: {e}")

            # try:
            #     sns.kdeplot(green_values, ax=ax, color="green",
            #                 fill=True, bw_adjust=0.5, label="KDE (200)")
            # except Exception as e:
            #     print(f"KDE error: {e}")

            # if len(red_values) >= 2:
            #     try:
            #         sns.kdeplot(
            #             red_values,
            #             ax=ax,
            #             color="red",
            #             fill=True,
            #             bw_adjust=0.5,
            #             label="KDE (400)"
            #         )
            #     except Exception as e:
            #         print(f"Red KDE error: {e}")

            ax.scatter(green_values, [0] * len(green_values),
                       color="green", s=20, label="200 OK")
            ax.scatter(red_values, [0] * len(red_values),
                       color="red", s=20, label="400 Error")

            ax.set_xlim(x_left, x_right)
            ax.legend()
            ax.grid(True)

            plt.draw()
            plt.pause(0.01)
except KeyboardInterrupt:
    print("\nStopped by user. Saving final plot...")

    # Save the final image
    timestamp = int(time.time())
    output_file = f"final_plot_{timestamp}.png"
    fig.savefig(output_file)
    print(f"Plot saved as {output_file}")

    # Keep window open until manually closed
    print("You can now interact with the plot window.")
    plt.ioff()
    plt.show()
