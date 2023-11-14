import numpy as np
import matplotlib.pyplot as plt
# Given data  
with open('predictionData_leadTimeForChanges.txt', 'r') as file:
    lines = file.readlines()
leadTime = []

for line in lines:
    freq_part = line.split(' = ')
    leadTime.append(float(freq_part[1]))


# Calculate Z-score
def z_score(data):
    mean = np.mean(data)
    std_dev = np.std(data)
    z_scores = [(x - mean) / std_dev for x in data]
    return z_scores

# Set threshold (adjust as needed)
threshold = 2

# Calculate Z-scores for runtime and CVSS
z_scores_deployment = z_score(leadTime)
# Get indices of outliers for both datasets
outlier_indices_runtime = np.where(np.abs(z_scores_deployment) > threshold)
x_values = [day for i, day in enumerate(range(1, len(leadTime) + 1)) if i not in outlier_indices_runtime[0]]

# Remove outliers from both datasets
cleaned_runtime_EWMA = [leadTime[i] for i in range(len(leadTime)) if i not in outlier_indices_runtime]
# for polynomial
cleaned_runtime_polynomial = cleaned_runtime_EWMA
# for linear
cleaned_runtime_linear = cleaned_runtime_EWMA
# ------------------EWMA--------------------
def calculate_ewma_weights(alpha, num_points):
    weights = [(1 - alpha)**t for t in reversed(range(num_points))]
    total_weight = sum(weights)
    normalized_weights = [w / total_weight for w in weights]
    return np.array(normalized_weights)

# Example usage
num_points = len(cleaned_runtime_EWMA) 
alpha = 0.3  # Smoothing parameter (adjust as needed)
weights = calculate_ewma_weights(alpha, num_points)
def wma(data, weights):
    n = len(data)
    smoothed_data = np.zeros(n)
    for i in range(n):
        smoothed_data[i] = np.sum(data[max(0, i-n+1):i+1] * weights[max(n-i-1, 0):])
    return smoothed_data / np.sum(weights[:n])


smoothed_vulnerability_wma = wma(cleaned_runtime_EWMA, weights)

# Calculate the residuals for WMA
residuals_wma = cleaned_runtime_EWMA[len(weights)-1:] - smoothed_vulnerability_wma

# Calculate the sum of squared residuals (SSR) for WMA
SSR_wma = np.sum(residuals_wma**2)

# Calculate the variance (mean squared error) for WMA
variance_wma = SSR_wma / len(residuals_wma)


def forecast_wma(data, weights, num_forecast):
    n = len(data)
    smoothed_data = np.zeros(n + num_forecast)
    smoothed_data[:n] = wma(data, weights)
    for i in range(n, n + num_forecast):
        smoothed_data[i] = np.sum(smoothed_data[i-n:i] * weights[-min(i, n):])
    smoothed_data[smoothed_data < 0] = 0
    return smoothed_data[n:]

# Number of points to forecast
num_forecast = 3

# Forecast the next three points
forecasted_points = forecast_wma(cleaned_runtime_EWMA, weights, num_forecast)

# ------------------linear--------------------
# Using numpy to perform linear regression
coefficients_linear = np.polyfit(x_values, cleaned_runtime_linear, 1)
poly_linear = np.poly1d(coefficients_linear)

# Calculate the residuals for linear regression
residuals_linear = cleaned_runtime_linear - poly_linear(cleaned_runtime_linear)

# Calculate the sum of squared residuals (SSR) for linear regression
SSR_linear = np.sum(residuals_linear**2)

# Take the natural logarithm of SSR for linear regression
log_SSR_linear = np.log(SSR_linear)

# ------------------polynomial--------------------
# Using numpy to perform polynomial regression (degree = 2)
coefficients_poly = np.polyfit(x_values, cleaned_runtime_polynomial, 2)
poly_poly = np.poly1d(coefficients_poly)

# Calculate the residuals for polynomial regression
residuals_poly = cleaned_runtime_polynomial - poly_poly(cleaned_runtime_polynomial)

# Calculate the sum of squared residuals (SSR) for polynomial regression
SSR_poly = np.sum(residuals_poly**2)

# Take the natural logarithm of SSR for polynomial regression
log_SSR_poly = np.log(SSR_poly)
poly = None


# # Plotting the data points in the original order
# for i in range(len(cleaned_runtime_polynomial)):
#     plt.scatter(cleaned_runtime_polynomial[i], cleaned_CVSS_linear[i], color='blue', label='Data Points' if i == 0 else '')

# ------------------linear regression--------------------
plt.figure()  # Create a new figure for linear regression
# Plotting the data points in the original order
plt.scatter(x_values, cleaned_runtime_linear, color='blue', label='Data Points', s=30)

# Plotting the regression line
largest_value = max(x_values)

# Calculate the next three days
next_days = np.array([largest_value + 1, largest_value + 2, largest_value + 3])

predicted_CVSS = poly_linear(next_days)
predicted_CVSS[predicted_CVSS < 0] = 0

plt.plot(x_values, poly_linear(x_values), color='green', label='Linear Regression Line')

# Plotting the predicted points
plt.scatter(next_days, predicted_CVSS, color='red', label='Predicted Lead Time For Change')

# Adding labels and legend
plt.xlabel('Days (Excluding Outliers)')
plt.ylabel('Lead Time For Change')
plt.legend()
plt.title('Lead Time For Change (linear)')

# Set y-axis limit to start from 0
max_value = max(cleaned_runtime_linear)
max_value_predict = max(predicted_CVSS)
chosen_max_value = max(max_value, max_value_predict)
upper_limit = chosen_max_value + 10
plt.ylim(0, upper_limit)

# plt.xticks(range(1, len(x_values)+1))
# Saving the plot as an image
plt.savefig('LeadTimeForChange_linear.png')


# ------------------polynomial regression--------------------
plt.figure()  # Create a new figure for polynomial regression

# Plotting the data points in the original order
plt.scatter(x_values, cleaned_runtime_polynomial, color='blue', label='Data Points')

# Plotting the regression line
x_range = np.linspace(min(x_values), max(x_values), 100)  # Generate a range of x values
regression_line = poly_poly(x_range)  # Calculate corresponding y values
plt.plot(x_range, regression_line, color='green', label='Polynomial Regression Line')

# Plotting the predicted points
largest_value = max(x_values)

# Calculate the next three days
next_days = np.array([largest_value + 1, largest_value + 2, largest_value + 3])

predicted_CVSS = poly_poly(next_days)
predicted_CVSS[predicted_CVSS < 0] = 0
plt.scatter(next_days, predicted_CVSS, color='red', label='Predicted Lead Time For Change')

# Adding labels and legend
plt.xlabel('Days (Excluding Outliers)')
plt.ylabel('Lead Time For Change')
plt.legend()
plt.title('Lead Time For Change (polynomial)')

# Set y-axis limit to start from 0
max_value = max(cleaned_runtime_linear)
max_value_predict = max(predicted_CVSS)
chosen_max_value = max(max_value, max_value_predict)
upper_limit = chosen_max_value + 10

plt.ylim(0, upper_limit)

# Saving the plot as an image
plt.savefig('LeadTimeForChange_polynomial.png')


# ------------------weighted moving average--------------------
plt.figure()  # Create a new figure for weighted moving average

# Plotting the data points in the original order
plt.scatter(x_values, cleaned_runtime_EWMA, color='blue', label='Data Points')

# Plotting the WMA smoothing
plt.plot(cleaned_runtime_EWMA, color='green', label='WMA Smoothing')

# Adding forecasted points to the plot
largest_value = max(x_values)
forecasted_x = np.arange(largest_value+1, largest_value + num_forecast+1)
plt.scatter(forecasted_x, forecasted_points, color='red', label='Predicted Lead Time For Change', marker='o')

# Adding labels and legend
plt.xlabel('Days (Excluding Outliers)')
plt.ylabel('Lead Time For Change')
plt.legend()
plt.title('Lead Time For Change Prediction (EWMA)')
max_value = max(cleaned_runtime_EWMA)
max_value_predict = max(forecasted_points)
chosen_max_value = max(max_value, max_value_predict)
upper_limit = chosen_max_value + 10

# Set y-axis limit to start from 0
plt.ylim(0, upper_limit)

# Saving the plot as an image
plt.savefig('LeadTimeForChange_wma.png')
